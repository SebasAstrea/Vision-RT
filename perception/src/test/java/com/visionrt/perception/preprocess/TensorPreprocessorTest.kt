package com.visionrt.perception.preprocess

import com.visionrt.perception.PerceptionFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TensorPreprocessorTest {

    @Test
    fun downscaleFillsNchwBufferWithNormalizedRgb() {
        // 2x2 source → 1x1 output takes top-left pixel; layout is CHW.
        val rgb = byteArrayOf(
            255.toByte(), 0, 0, // (0,0) red
            0, 255.toByte(), 0,
            0, 0, 255.toByte(),
            255.toByte(), 255.toByte(), 255.toByte(),
        )
        val frame = PerceptionFrame(1L, 2, 2, rgb)
        val pre = TensorPreprocessor(1, 1)
        val out = pre.newBuffer()
        assertTrue(pre.preprocess(frame, out))
        assertEquals(3, out.size)
        assertEquals(1f, out[0], 1e-5f) // R plane
        assertEquals(0f, out[1], 1e-5f) // G plane
        assertEquals(0f, out[2], 1e-5f) // B plane
    }

    @Test
    fun metadataOnlyFrameFails() {
        val frame = PerceptionFrame(1L, 640, 480)
        val pre = TensorPreprocessor(320, 320)
        assertFalse(pre.preprocess(frame, pre.newBuffer()))
    }

    @Test
    fun wrongOutputSizeFails() {
        val rgb = ByteArray(2 * 2 * 3)
        val frame = PerceptionFrame(1L, 2, 2, rgb)
        val pre = TensorPreprocessor(4, 4)
        assertFalse(pre.preprocess(frame, FloatArray(3)))
    }

    @Test
    fun inputSizeMatchesNchwLayout() {
        assertEquals(3 * 320 * 320, TensorPreprocessor(320, 320).inputSize)
    }
}
