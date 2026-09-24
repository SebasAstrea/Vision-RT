package com.visionrt.inference.litert

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import com.visionrt.core.common.SafeLogger
import com.visionrt.core.domain.Detection
import com.visionrt.inference.manifest.ModelManifest
import com.visionrt.inference.manifest.ModelManifestJson
import com.visionrt.inference.postprocess.DetectionPostProcessor
import com.visionrt.inference.runtimeapi.ModelConfig
import com.visionrt.inference.runtimeapi.ObjectDetector
import com.visionrt.perception.PerceptionFrame
import com.visionrt.perception.preprocess.TensorPreprocessor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * LiteRT/TFLite YOLOv8n detector adapter (ARCHITECTURE §11, M3 deliverable 5).
 *
 * - Loads assets under [MODEL_ASSET_DIR] validated by [ModelManifestJson].
 * - Input: float32 `[1, 3, H, W]` NCHW in [0,1] (XNNPACK CPU).
 * - Asset is INT8 weights with float activations (dynamic wi8); IO stays float.
 * - Output: uses the detection head `[1, 4+C, N]` (ultralytics export may
 *   expose extra feature-map outputs; only the flat head is consumed).
 * - Off-main-thread load/detect/unload via Dispatchers.Default + [lock]
 *   so stop() never closes the native interpreter mid-inference.
 * - Failure must not crash (OR-002.7): all entry points return Result.
 * - Constructed by app DI (Hilt lives outside inference).
 */
class LiteRtObjectDetector(
    private val assets: AssetManager,
) : ObjectDetector {

    private val lock = Mutex()

    @Volatile
    private var interpreter: Interpreter? = null

    @Volatile
    private var manifest: ModelManifest? = null

    @Volatile
    private var config: ModelConfig? = null

    private var preprocessor: TensorPreprocessor? = null
    private var inputBuffer: FloatArray? = null
    private var detectionOutput: Array<Array<FloatArray>>? = null
    private var detectionOutputIndex: Int = -1

    override fun isLoaded(): Boolean = interpreter != null

    override suspend fun load(config: ModelConfig): Result<Unit> =
        withContext(Dispatchers.Default) {
            lock.withLock {
                runCatching {
                    unloadInternal()
                    val assetName = resolveAssetName()
                    val model = loadMappedAsset(assetName)
                    val opts = Interpreter.Options().apply { setNumThreads(THREADS) }
                    val interp = Interpreter(model, opts)

                    val manifestJson = readAssetText(MANIFEST_ASSET)
                    val parsed = ModelManifestJson.parse(manifestJson)
                        .getOrElse { error("Invalid manifest: ${it.message}") }
                    if (parsed.inputWidth != config.inputWidth ||
                        parsed.inputHeight != config.inputHeight
                    ) {
                        error(
                            "Manifest input ${parsed.inputWidth}x${parsed.inputHeight} " +
                                "does not match config ${config.inputWidth}x${config.inputHeight}",
                        )
                    }

                    val inputTensor = interp.getInputTensor(0)
                    val inShape = inputTensor.shape()
                    val expectedIn = intArrayOf(
                        BATCH, RGB_CHANNELS, config.inputHeight, config.inputWidth,
                    )
                    require(inShape.contentEquals(expectedIn)) {
                        "Expected NCHW input ${expectedIn.contentToString()}, " +
                            "got ${inShape.contentToString()}"
                    }

                    val outs = (0 until interp.outputTensorCount).map { i ->
                        interp.getOutputTensor(i)
                    }
                    val expectedChannels = BOX_DIMS + parsed.classes.size
                    val detIndex = outs.indexOfFirst { t ->
                        val s = t.shape()
                        s.size == RANK && s[CHANNEL_AXIS] == expectedChannels && s[0] == BATCH
                    }
                    require(detIndex >= 0) {
                        "No detection head with channels $expectedChannels among outputs"
                    }
                    val detShape = outs[detIndex].shape()
                    val channels = detShape[CHANNEL_AXIS]
                    val anchors = detShape[ANCHOR_AXIS]

                    preprocessor = TensorPreprocessor(config.inputWidth, config.inputHeight)
                    inputBuffer = FloatArray(preprocessor!!.inputSize)
                    detectionOutput = Array(BATCH) { Array(channels) { FloatArray(anchors) } }
                    detectionOutputIndex = detIndex

                    interpreter = interp
                    this@LiteRtObjectDetector.manifest = parsed
                    this@LiteRtObjectDetector.config = config
                    SafeLogger.i(TAG, "Loaded $assetName ${config.modelId}@${config.modelVersion}")
                    Unit
                }
            }
        }

    override suspend fun detect(frame: PerceptionFrame): Result<List<Detection>> =
        withContext(Dispatchers.Default) {
            lock.withLock {
                runCatching {
                    val interp = interpreter ?: error("Detector not loaded")
                    val m = manifest ?: error("Manifest missing")
                    val pre = preprocessor ?: error("Preprocessor missing")
                    val input = inputBuffer ?: error("Input buffer missing")
                    val output = detectionOutput ?: error("Output buffer missing")
                    val detIndex = detectionOutputIndex
                    if (detIndex < 0) error("Detection output index missing")

                    if (!pre.preprocess(frame, input)) {
                        return@runCatching emptyList<Detection>()
                    }

                    val inputTensor = ByteBuffer.allocateDirect(input.size * FLOAT_BYTES)
                        .order(ByteOrder.nativeOrder())
                    inputTensor.asFloatBuffer().put(input)
                    inputTensor.rewind()

                    val outputs = hashMapOf<Int, Any>(detIndex to output)
                    interp.runForMultipleInputsOutputs(arrayOf(inputTensor), outputs)
                    var maxScore = 0f
                    var aboveThreshold = 0
                    for (c in 4 until output[0].size) {   // c=0..3 son box coords
                        for (a in 0 until output[0][c].size) {
                            val v = output[0][c][a]
                            if (v > maxScore) maxScore = v
                                if (v >= m.confidenceThreshold) aboveThreshold++
                        }
                    }
                    SafeLogger.i(
                        TAG,
                        "detect: maxScore=${"%.4f".format(maxScore)} above=${aboveThreshold} frame=${frame.timestampMs}"
                    )

                    val channels = output[0].size
                    val anchors = output[0][0].size
                    val flat = FloatArray(channels * anchors)
                    for (c in 0 until channels) {
                        System.arraycopy(output[0][c], 0, flat, c * anchors, anchors)
                    }

                    DetectionPostProcessor.process(
                        output = flat,
                        channels = channels,
                        anchors = anchors,
                        manifest = m,
                        timestampMs = frame.timestampMs,
                    )
                }
            }
        }

    override suspend fun unload() = withContext(Dispatchers.Default) {
        lock.withLock {
            unloadInternal()
        }
    }

    private fun unloadInternal() {
        try {
            interpreter?.close()
        } catch (_: Exception) {
            // already closed
        }
        interpreter = null
        manifest = null
        config = null
        preprocessor = null
        inputBuffer = null
        detectionOutput = null
        detectionOutputIndex = -1
    }

    private fun resolveAssetName(): String {
        val fromManifest = runCatching {
            ModelManifestJson.parse(readAssetText(MANIFEST_ASSET)).getOrThrow().modelAsset
        }.getOrNull()
        return fromManifest?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL_ASSET
    }

    private fun loadMappedAsset(name: String): MappedByteBuffer {
        val fd: AssetFileDescriptor = assets.openFd("$MODEL_ASSET_DIR/$name")
        FileInputStream(fd.fileDescriptor).use { stream ->
            val channel = stream.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    private fun readAssetText(path: String): String =
        assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        private const val TAG = "LiteRtObjectDetector"
        private const val THREADS = 2
        private const val RANK = 3
        private const val BATCH = 1
        private const val CHANNEL_AXIS = 1
        private const val ANCHOR_AXIS = 2
        private const val RGB_CHANNELS = 3
        private const val FLOAT_BYTES = 4
        private const val BOX_DIMS = 4
        const val MODEL_ASSET_DIR = "models"
        const val MANIFEST_ASSET = "$MODEL_ASSET_DIR/manifest.json"
        const val DEFAULT_MODEL_ASSET = "yolov8n_320_int8.tflite"
    }
}
