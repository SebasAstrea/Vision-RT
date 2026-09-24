package com.visionrt.core.ocr

import com.visionrt.core.ocr.fake.FakeTextRecognizer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognizerOcrSourceTest {

    private fun capture() = TextCapture(2, 2, ByteArray(12))

    /** Minimal lifecycle double that records touches. */
    private class CountingLifecycle(private val recognizer: FakeTextRecognizer) : OcrLifecycle {
        var touches = 0
        var ensureCalls = 0
        override suspend fun ensureLoaded(): Result<Unit> {
            ensureCalls++
            return if (recognizer.isLoaded()) Result.success(Unit)
            else recognizer.load()
        }

        override fun touch() {
            touches++
        }

        override suspend fun unload() = recognizer.unload()

        override fun isLoaded(): Boolean = recognizer.isLoaded()
    }

    @Test
    fun recognizeLazyLoadsAndReturnsJoinedText() = runTest {
        val fake = FakeTextRecognizer()
        val life = CountingLifecycle(fake)
        val source = RecognizerOcrSource(fake, life) { capture() }
        fake.stageBlocks(
            listOf(
                TextBlock("line one", 0),
                TextBlock("line two", 1),
            ),
        )
        val text = source.recognize().getOrThrow()
        assertEquals("line one\nline two", text)
        assertTrue(fake.isLoaded())
        assertEquals(1, life.ensureCalls)
        assertTrue(life.touches >= 2)
    }

    @Test
    fun missingCaptureFailsWithoutLoggingText() = runTest {
        val fake = FakeTextRecognizer()
        val life = CountingLifecycle(fake)
        val source = RecognizerOcrSource(fake, life) { null }
        assertTrue(source.recognize().isFailure)
        assertEquals(0, fake.recognizeCallCount)
    }

    @Test
    fun emptyResultMapsToEmptyString() = runTest {
        val fake = FakeTextRecognizer()
        val life = CountingLifecycle(fake)
        val source = RecognizerOcrSource(fake, life) { capture() }
        // staged empty → recognize returns empty blocks
        val text = source.recognize().getOrThrow()
        assertEquals("", text)
    }

    @Test
    fun ensureLoadedFailurePropagates() = runTest {
        val fake = FakeTextRecognizer()
        val life = object : OcrLifecycle {
            override suspend fun ensureLoaded(): Result<Unit> =
                Result.failure(IllegalStateException("load failed"))
            override fun touch() = Unit
            override suspend fun unload() = Unit
            override fun isLoaded() = false
        }
        val source = RecognizerOcrSource(fake, life) { capture() }
        assertTrue(source.recognize().isFailure)
        assertFalse(fake.isLoaded())
    }
}
