package com.visionrt.perception.preprocess

import com.visionrt.perception.PerceptionFrame
import kotlin.math.min

/**
 * Downscales a frame's RGB888 buffer into a reusable detector input tensor
 * (ARCHITECTURE §10.2). Nearest-neighbour sampling, no Bitmap, no per-frame
 * allocation of the output (caller reuses [output]).
 *
 * Layout matches LiteRT YOLOv8n NCHW float input `[1, 3, H, W]` with values
 * in [0,1]: index = ((c * outH + y) * outW + x) for c in R,G,B.
 */
class TensorPreprocessor(
    private val outWidth: Int,
    private val outHeight: Int,
) {
    private val planeSize = outWidth * outHeight
    private val greenOffset = 1
    private val blueOffset = 2
    private val bluePlane = 2
    @Volatile var lastScale: Float = 1f
    private set
    @Volatile var lastPadX: Float = 0f
    private set
    @Volatile var lastPadY: Float = 0f
    private set

    val inputSize: Int = RGB_CHANNELS * planeSize

    fun newBuffer(): FloatArray = FloatArray(inputSize)

    /**
     * Fills [output] (length [inputSize]) from [frame.rgb888].
     * Returns false if the frame has no pixel payload or wrong dimensions.
     */
    fun preprocess(frame: PerceptionFrame, output: FloatArray): Boolean {
        val rgb = frame.rgb888
        val ok = rgb != null &&
            output.size == inputSize &&
            rgb.size == PerceptionFrame.expectedRgbSize(frame.width, frame.height)
        if (ok && rgb != null) {
            fill(rgb, frame.width, frame.height, output)
        }
        return ok
    }

    private fun fill(rgb: ByteArray, srcW: Int, srcH: Int, output: FloatArray) {
        // 1. Calcular escala y padding centrado
        val scale = minOf(outWidth.toFloat() / srcW, outHeight.toFloat() / srcH)
        val newW = (srcW * scale).toInt()
        val newH = (srcH * scale).toInt()
        val padX = (outWidth - newW) / 2
        val padY = (outHeight - newH) / 2

        // 2. Exponer transformación (el postprocesado la necesita)
        lastScale = scale
        lastPadX = padX.toFloat()
        lastPadY = padY.toFloat()

        // 3. Rellenar con gris (114/255 ≈ 0.447, el valor estándar de YOLO)
        java.util.Arrays.fill(output, PAD_VALUE)

        // 4. Bilinear sample dentro de la región útil
        for (y in 0 until newH) {
            val srcYf = y / scale
            val y0 = srcYf.toInt().coerceIn(0, srcH - 1)
            val y1 = (y0 + 1).coerceAtMost(srcH - 1)
            val wy = srcYf - y0

            for (x in 0 until newW) {
                val srcXf = x / scale
                val x0 = srcXf.toInt().coerceIn(0, srcW - 1)
                val x1 = (x0 + 1).coerceAtMost(srcW - 1)
                val wx = srcXf - x0

                val dst = (y + padY) * outWidth + (x + padX)

                for (c in 0 until RGB_CHANNELS) {
                    val p00 = rgb[(y0 * srcW + x0) * RGB_CHANNELS + c].toInt() and BYTE_MASK
                    val p01 = rgb[(y0 * srcW + x1) * RGB_CHANNELS + c].toInt() and BYTE_MASK
                    val p10 = rgb[(y1 * srcW + x0) * RGB_CHANNELS + c].toInt() and BYTE_MASK
                    val p11 = rgb[(y1 * srcW + x1) * RGB_CHANNELS + c].toInt() and BYTE_MASK
                    val top = p00 + (p01 - p00) * wx
                    val bot = p10 + (p11 - p10) * wx
                    val v = (top + (bot - top) * wy) / RGB_SCALE
                    // NCHW: canal c ocupa el plano c * planeSize
                    output[c * planeSize + dst] = v
                }
            }
        }
    }

    private companion object {
        const val RGB_CHANNELS = 3
        const val PAD_VALUE = 114f / 255f
        const val BYTE_MASK = 0xFF
        const val RGB_SCALE = 255f
    }
}
