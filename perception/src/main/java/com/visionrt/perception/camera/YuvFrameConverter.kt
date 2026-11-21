package com.visionrt.perception.camera

import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Converts YUV_420_888 planes into packed RGB888 while:
 *
 * 1. Rotating the frame upright with CameraX `ImageInfo.rotationDegrees`.
 * 2. Scaling the whole sensor frame into a bounded analysis budget instead of
 *    cropping the top-left region when the camera delivers more than 640x480.
 *
 * Single pass, no Bitmap, no per-pixel allocations. Pure JVM so the geometry
 * and rotation can be unit-tested without a device (ARCHITECTURE §10.1-10.3).
 */
object YuvFrameConverter {

    data class Plane(
        val buffer: ByteBuffer,
        val rowStride: Int,
        val pixelStride: Int,
    ) {
        val capacity: Int = buffer.capacity()
    }

    data class Source(
        val y: Plane,
        val u: Plane,
        val v: Plane,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
    )

    /** Upper bounds for the analysis frame; keeps reused buffers bounded. */
    data class Budget(
        val maxLongSide: Int,
        val maxPixels: Int,
    )

    data class Size(val width: Int, val height: Int) {
        val pixelCount: Int get() = width * height
    }

    /**
     * Output size after rotation and uniform downscale. Aspect ratio is
     * preserved: the long side never exceeds [Budget.maxLongSide] and the
     * pixel count never exceeds [Budget.maxPixels]. Rotation by 90/270 swaps
     * width and height.
     */
    fun outputSize(
        srcWidth: Int,
        srcHeight: Int,
        rotationDegrees: Int,
        budget: Budget,
    ): Size {
        require(srcWidth > 0 && srcHeight > 0) { "Source size must be positive" }
        require(budget.maxLongSide > 0 && budget.maxPixels > 0) { "Budget must be positive" }

        val longSideScale = budget.maxLongSide.toFloat() / max(srcWidth, srcHeight)
        val areaScale = sqrt(budget.maxPixels.toFloat() / (srcWidth.toFloat() * srcHeight))
        val scale = min(1f, min(longSideScale, areaScale))

        val width = (srcWidth * scale).roundToInt().coerceAtLeast(1)
        val height = (srcHeight * scale).roundToInt().coerceAtLeast(1)
        return if (normalize(rotationDegrees) % HALF_TURN == 0) {
            Size(width, height)
        } else {
            Size(height, width)
        }
    }

    /**
     * Fills [out] with packed RGB888. Returns the produced [Size], or null when
     * [out] is too small for the computed geometry.
     */
    fun convert(source: Source, budget: Budget, out: ByteArray): Size? {
        val size = outputSize(source.width, source.height, source.rotationDegrees, budget)
        if (out.size < size.pixelCount * RGB_CHANNELS) return null

        val rotation = normalize(source.rotationDegrees)
        var dst = 0
        for (oy in 0 until size.height) {
            val destV = (oy + HALF_PIXEL) / size.height
            for (ox in 0 until size.width) {
                val destU = (ox + HALF_PIXEL) / size.width

                // Rotate the destination point back into source space.
                var srcU = destU
                var srcV = destV
                when (rotation) {
                    QUARTER_TURN -> {
                        srcU = destV
                        srcV = 1f - destU
                    }
                    HALF_TURN -> {
                        srcU = 1f - destU
                        srcV = 1f - destV
                    }
                    THREE_QUARTER_TURN -> {
                        srcU = 1f - destV
                        srcV = destU
                    }
                }

                val sx = srcU * source.width - HALF_PIXEL
                val sy = srcV * source.height - HALF_PIXEL
                val luma = bilinearLuma(source.y, sx, sy, source.width, source.height)

                // 4:2:0: one chroma sample per 2x2 luma block.
                val chromaX = (srcU * source.width).toInt()
                    .coerceIn(0, source.width - 1) / CHROMA_DIVISOR
                val chromaY = (srcV * source.height).toInt()
                    .coerceIn(0, source.height - 1) / CHROMA_DIVISOR
                val uValue = chroma(source.u, chromaX, chromaY) - COLOR_OFFSET
                val vValue = chroma(source.v, chromaX, chromaY) - COLOR_OFFSET

                val red = luma + R_V * vValue
                val green = luma + G_U * uValue + G_V * vValue
                val blue = luma + B_U * uValue

                out[dst++] = clamp8(red)
                out[dst++] = clamp8(green)
                out[dst++] = clamp8(blue)
            }
        }
        return size
    }

    private fun bilinearLuma(
        plane: Plane,
        sx: Float,
        sy: Float,
        width: Int,
        height: Int,
    ): Float {
        val x0 = floor(sx).toInt()
        val y0 = floor(sy).toInt()
        val wx = sx - x0
        val wy = sy - y0

        val p00 = luma(plane, x0, y0, width, height)
        val p10 = luma(plane, x0 + 1, y0, width, height)
        val p01 = luma(plane, x0, y0 + 1, width, height)
        val p11 = luma(plane, x0 + 1, y0 + 1, width, height)

        val top = p00 + (p10 - p00) * wx
        val bottom = p01 + (p11 - p01) * wx
        return top + (bottom - top) * wy
    }

    private fun luma(plane: Plane, x: Int, y: Int, width: Int, height: Int): Float {
        val cx = x.coerceIn(0, width - 1)
        val cy = y.coerceIn(0, height - 1)
        return byteAt(plane, cy * plane.rowStride + cx * plane.pixelStride).toFloat()
    }

    private fun chroma(plane: Plane, x: Int, y: Int): Int =
        byteAt(plane, y * plane.rowStride + x * plane.pixelStride)

    private fun byteAt(plane: Plane, index: Int): Int {
        val safeIndex = index.coerceIn(0, plane.capacity - 1)
        return plane.buffer.get(safeIndex).toInt() and BYTE_MASK
    }

    private fun clamp8(value: Float): Byte =
        value.toInt().coerceIn(0, MAX_8BIT).toByte()

    private fun normalize(rotationDegrees: Int): Int {
        val degrees = ((rotationDegrees % FULL_TURN) + FULL_TURN) % FULL_TURN
        val rounded = (degrees + RIGHT_ANGLE / 2) / RIGHT_ANGLE * RIGHT_ANGLE
        return rounded % FULL_TURN
    }

    private const val RGB_CHANNELS = 3
    private const val HALF_PIXEL = 0.5f
    private const val CHROMA_DIVISOR = 2
    private const val COLOR_OFFSET = 128
    private const val MAX_8BIT = 255
    private const val BYTE_MASK = 0xFF
    private const val R_V = 1.402f
    private const val G_U = -0.344136f
    private const val G_V = -0.714136f
    private const val B_U = 1.772f
    private const val RIGHT_ANGLE = 90
    private const val QUARTER_TURN = 90
    private const val HALF_TURN = 180
    private const val THREE_QUARTER_TURN = 270
    private const val FULL_TURN = 360
}
