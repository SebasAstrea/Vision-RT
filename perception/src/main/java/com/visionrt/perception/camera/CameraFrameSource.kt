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
import com.visionrt.core.common.SafeLogger
import com.visionrt.perception.PerceptionFrame
import com.visionrt.perception.framegate.FrameGate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * CameraX analysis pipeline (ARCHITECTURE §6.3, §10.1): latest-only analysis,
 * YUV_420_888 → upright RGB888 into a reused buffer, submit to [FrameGate],
 * close the ImageProxy promptly. Never allocates a Bitmap per frame.
 *
 * Frames are rotated with `ImageInfo.rotationDegrees` and scaled to
 * [frameBudget]; a sensor frame larger than 640x480 is downscaled, never
 * cropped, so the full field of view reaches the detector.
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
    private var lastGeometry: String? = null

    private val frameBudget = YuvFrameConverter.Budget(
        maxLongSide = maxOf(analysisWidth, analysisHeight),
        maxPixels = analysisWidth * analysisHeight,
    )

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
            val buffer = rgbBuffer ?: return
            val planes = image.planes
            val size = YuvFrameConverter.convert(
                source = YuvFrameConverter.Source(
                    y = planes[0].toConverterPlane(),
                    u = planes[1].toConverterPlane(),
                    v = planes[2].toConverterPlane(),
                    width = image.width,
                    height = image.height,
                    rotationDegrees = image.imageInfo.rotationDegrees,
                ),
                budget = frameBudget,
                out = buffer,
            ) ?: return

            logGeometryOnce(image, size)
            frameGate.submit(
                PerceptionFrame(
                    timestampMs = image.imageInfo.timestamp / NANOS_PER_MILLIS,
                    width = size.width,
                    height = size.height,
                    rgb888 = buffer.copyOf(size.pixelCount * BYTES_PER_PIXEL),
                ),
            )
        } finally {
            image.close()
        }
    }

    private fun ImageProxy.PlaneProxy.toConverterPlane(): YuvFrameConverter.Plane =
        YuvFrameConverter.Plane(buffer = buffer, rowStride = rowStride, pixelStride = pixelStride)

    private fun logGeometryOnce(image: ImageProxy, size: YuvFrameConverter.Size) {
        val geometry = "${image.width}x${image.height}@${image.imageInfo.rotationDegrees}" +
            "->${size.width}x${size.height}"
        if (geometry != lastGeometry) {
            lastGeometry = geometry
            SafeLogger.i(TAG, "frame geometry $geometry")
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
        private const val TAG = "CameraFrameSource"
        private const val BYTES_PER_PIXEL = 3
        private const val NANOS_PER_MILLIS = 1_000_000L
    }
}
