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
        val scaleW = srcW.toFloat() / outWidth
        val scaleH = srcH.toFloat() / outHeight
        for (y in 0 until outHeight) {
            val srcY = min((y * scaleH).toInt(), srcH - 1)
            val rowBase = srcY * srcW * RGB_CHANNELS
            for (x in 0 until outWidth) {
                val srcX = min((x * scaleW).toInt(), srcW - 1)
                val src = rowBase + srcX * RGB_CHANNELS
                val dst = y * outWidth + x
                output[dst] = (rgb[src].toInt() and BYTE_MASK) / RGB_SCALE
                output[planeSize + dst] = (rgb[src + greenOffset].toInt() and BYTE_MASK) / RGB_SCALE
                output[bluePlane * planeSize + dst] =
                    (rgb[src + blueOffset].toInt() and BYTE_MASK) / RGB_SCALE
            }
        }
    }

    private companion object {
        const val RGB_CHANNELS = 3
        const val BYTE_MASK = 0xFF
        const val RGB_SCALE = 255f
    }
}
