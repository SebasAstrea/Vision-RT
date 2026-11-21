package com.visionrt.perception.camera

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YuvFrameConverterTest {

    @Test
    fun outputSizePreservesAspectAndCapsLongSide() {
        val budget = YuvFrameConverter.Budget(maxLongSide = 640, maxPixels = 640 * 480)

        assertEquals(
            YuvFrameConverter.Size(640, 360),
            YuvFrameConverter.outputSize(1280, 720, 0, budget),
        )
        assertEquals(
            YuvFrameConverter.Size(360, 640),
            YuvFrameConverter.outputSize(1280, 720, 90, budget),
        )
        assertEquals(
            YuvFrameConverter.Size(640, 480),
            YuvFrameConverter.outputSize(640, 480, 0, budget),
        )
        assertEquals(
            YuvFrameConverter.Size(480, 640),
            YuvFrameConverter.outputSize(640, 480, 90, budget),
        )
    }

    @Test
    fun outputSizeRespectsPixelBudgetForSquareSensor() {
        val size = YuvFrameConverter.outputSize(
            srcWidth = 800,
            srcHeight = 800,
            rotationDegrees = 0,
            budget = YuvFrameConverter.Budget(maxLongSide = 640, maxPixels = 640 * 480),
        )

        assertTrue(size.pixelCount <= 640 * 480)
        assertEquals(size.width, size.height)
    }

    @Test
    fun wideFrameIsScaledNotCropped() {
        // Left half dark, right half bright. The old top-left crop returned
        // only dark pixels; scaling must keep both halves in the output.
        val source = source(8, 4) { x, _ -> if (x < 4) 10 else 200 }
        val out = ByteArray(4 * 2 * 3)

        val size = YuvFrameConverter.convert(
            source = source,
            budget = YuvFrameConverter.Budget(maxLongSide = 4, maxPixels = 4 * 2),
            out = out,
        )

        assertEquals(4, size?.width)
        assertEquals(2, size?.height)
        assertEquals(10, out[0].toInt() and 0xFF) // left edge
        assertEquals(200, out[9].toInt() and 0xFF) // right edge, row 0
    }

    @Test
    fun rotation90MapsSourceQuadrantsClockwise() {
        // Source: 10 20 / 30 40. Rotated 90 CW: 30 10 / 40 20.
        val source = source(2, 2) { x, y ->
            when {
                x == 0 && y == 0 -> 10
                x == 1 && y == 0 -> 20
                x == 0 && y == 1 -> 30
                else -> 40
            }
        }.copy(rotationDegrees = 90)
        val out = ByteArray(2 * 2 * 3)

        val size = YuvFrameConverter.convert(
            source = source,
            budget = YuvFrameConverter.Budget(maxLongSide = 2, maxPixels = 4),
            out = out,
        )

        assertEquals(2, size?.width)
        assertEquals(30, out[0].toInt() and 0xFF)
        assertEquals(10, out[3].toInt() and 0xFF)
        assertEquals(40, out[6].toInt() and 0xFF)
        assertEquals(20, out[9].toInt() and 0xFF)
    }

    @Test
    fun tooSmallOutputReturnsNull() {
        val source = source(8, 4) { _, _ -> 0 }

        assertNull(
            YuvFrameConverter.convert(
                source = source,
                budget = YuvFrameConverter.Budget(maxLongSide = 8, maxPixels = 8 * 4),
                out = ByteArray(3),
            ),
        )
    }

    private fun source(
        width: Int,
        height: Int,
        luma: (x: Int, y: Int) -> Int,
    ): YuvFrameConverter.Source {
        val y = ByteBuffer.allocate(width * height)
        for (row in 0 until height) {
            for (column in 0 until width) {
                y.put(row * width + column, luma(column, row).toByte())
            }
        }

        val chromaWidth = (width + 1) / 2
        val chromaHeight = (height + 1) / 2
        val u = ByteBuffer.allocate(chromaWidth * chromaHeight)
        val v = ByteBuffer.allocate(chromaWidth * chromaHeight)
        for (index in 0 until chromaWidth * chromaHeight) {
            u.put(index, NEUTRAL_CHROMA.toByte())
            v.put(index, NEUTRAL_CHROMA.toByte())
        }

        return YuvFrameConverter.Source(
            y = YuvFrameConverter.Plane(y, width, 1),
            u = YuvFrameConverter.Plane(u, chromaWidth, 1),
            v = YuvFrameConverter.Plane(v, chromaWidth, 1),
            width = width,
            height = height,
            rotationDegrees = 0,
        )
    }

    private companion object {
        const val NEUTRAL_CHROMA = 128
    }
}
