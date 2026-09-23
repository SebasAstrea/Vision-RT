package com.visionrt.perception.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.visionrt.perception.PerceptionFrame
import com.visionrt.perception.framegate.FrameGate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * CameraX analysis pipeline (ARCHITECTURE §6.3, §10.1): 640×480 latest-only,
 * YUV_420_888 → RGB888 into a reused buffer, submit to [FrameGate], close the
 * ImageProxy promptly. Never allocates a Bitmap per frame.
 */
class CameraFrameSource(
    private val context: Context,
    private val frameGate: FrameGate,
    private val analysisWidth: Int = ANALYSIS_WIDTH,
    private val analysisHeight: Int = ANALYSIS_HEIGHT,
) {
    private var provider: ProcessCameraProvider? = null
    private var executor: ExecutorService? = null
    private var rgbBuffer: ByteArray? = null

    @Volatile
    private var bound = false

    val isBound: Boolean get() = bound

    /** Peeks the latest gated frame without consuming it. */
    fun peekLatestFrame() = frameGate.latest()

    /** Consumes the latest gated frame (shared with DetectionSource). */
    fun consumeLatestFrame() = frameGate.consume()

    /** Drops any pending frame without running inference. */
    fun clearFrames() = frameGate.clear()

    /** Binds rear camera ImageAnalysis to [lifecycleOwner]. Idempotent. */
    suspend fun bind(lifecycleOwner: LifecycleOwner): Result<Unit> = runCatching {
        if (!bound) {
            bindInternal(lifecycleOwner)
        }
        Result.success(Unit)
    }.getOrElse { Result.failure(it) }

    /** Unbinds camera and releases the analysis executor. Safe to call twice. */
    fun unbind() {
        bound = false
        try {
            provider?.unbindAll()
        } catch (_: Exception) {
            // Provider may already be shut down.
        }
        executor?.shutdown()
        executor = null
        rgbBuffer = null
    }

    private suspend fun bindInternal(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = awaitProvider()
        provider = cameraProvider
        val analysisExecutor = Executors.newSingleThreadExecutor()
        executor = analysisExecutor

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(analysisWidth, analysisHeight),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        rgbBuffer = ByteArray(analysisWidth * analysisHeight * BYTES_PER_PIXEL)
        analysis.setAnalyzer(analysisExecutor) { image -> analyze(image) }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            analysis,
        )
        bound = true
    }

    private fun analyze(image: ImageProxy) {
        try {
            val buffer = rgbBuffer
            if (buffer != null && yuv420ToRgb888(image, buffer, analysisWidth, analysisHeight)) {
                frameGate.submit(
                    PerceptionFrame(
                        timestampMs = image.imageInfo.timestamp / NANOS_PER_MILLIS,
                        width = analysisWidth,
                        height = analysisHeight,
                        rgb888 = buffer.copyOf(),
                    ),
                )
            }
        } finally {
            image.close()
        }
    }

    private suspend fun awaitProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context),
        )
    }

    companion object {
        const val ANALYSIS_WIDTH = 640
        const val ANALYSIS_HEIGHT = 480
        private const val BYTES_PER_PIXEL = 3
        private const val NANOS_PER_MILLIS = 1_000_000L
        private const val UV_HALF = 2
        private const val COLOR_OFFSET = 128
        private const val MAX_8BIT = 255
        private const val Y_GAIN = 1f
        private const val R_V = 1.402f
        private const val G_U = -0.344136f
        private const val G_V = -0.714136f
        private const val B_U = 1.772f

        /**
         * YUV_420_888 → packed RGB888 (BT.601 full-range style).
         * Writes exactly [width]*[height]*3 bytes into [out].
         */
        fun yuv420ToRgb888(image: ImageProxy, out: ByteArray, width: Int, height: Int): Boolean {
            val planesOk = image.width >= width && image.height >= height &&
                out.size >= width * height * BYTES_PER_PIXEL
            if (planesOk) {
                convertPlanes(image, out, width, height)
            }
            return planesOk
        }

        private fun convertPlanes(
            image: ImageProxy,
            out: ByteArray,
            width: Int,
            height: Int,
        ) {
            val planeY = image.planes[0]
            val planeU = image.planes[1]
            val planeV = image.planes[2]
            val yBuf = planeY.buffer
            val uBuf = planeU.buffer
            val vBuf = planeV.buffer
            val yRowStride = planeY.rowStride
            val yPixelStride = planeY.pixelStride
            val uRowStride = planeU.rowStride
            val uPixelStride = planeU.pixelStride
            val vRowStride = planeV.rowStride
            val vPixelStride = planeV.pixelStride

            var outIndex = 0
            for (row in 0 until height) {
                val yRow = row * yRowStride
                val uvRow = (row / UV_HALF) * uRowStride
                val vRowBase = (row / UV_HALF) * vRowStride
                for (col in 0 until width) {
                    val y = yBuf.get(yRow + col * yPixelStride).toInt() and BYTE_MASK
                    val uIndex = uvRow + (col / UV_HALF) * uPixelStride
                    val u = (uBuf.get(uIndex).toInt() and BYTE_MASK) - COLOR_OFFSET
                    val vIndex = vRowBase + (col / UV_HALF) * vPixelStride
                    val v = (vBuf.get(vIndex).toInt() and BYTE_MASK) - COLOR_OFFSET

                    val r = (Y_GAIN * y + R_V * v).toInt().coerceIn(0, MAX_8BIT)
                    val g = (Y_GAIN * y + G_U * u + G_V * v).toInt().coerceIn(0, MAX_8BIT)
                    val b = (Y_GAIN * y + B_U * u).toInt().coerceIn(0, MAX_8BIT)

                    out[outIndex++] = r.toByte()
                    out[outIndex++] = g.toByte()
                    out[outIndex++] = b.toByte()
                }
            }
        }

        private const val BYTE_MASK = 0xFF
    }
}
