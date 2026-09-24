package com.visionrt.core.ocr

import com.visionrt.core.ocr.fake.FakeTextRecognizer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextReadingSessionTest {

    @Test
    fun loadOrdersBlocksAndStartsReading() {
        val session = TextReadingSession()
        val started = session.load(
            listOf(
                TextBlock("second", readingOrder = 1),
                TextBlock("first", readingOrder = 0),
            ),
        )
        assertTrue(started)
        assertTrue(session.isReading)
        assertEquals(0, session.currentIndex)
        assertEquals("first", session.currentBlock()?.text)
    }

    @Test
    fun nextAdvancesThenStopsAtEnd() {
        val session = TextReadingSession()
        session.load(
            listOf(
                TextBlock("a", 0),
                TextBlock("b", 1),
            ),
        )
        assertEquals("b", session.next())
        assertFalse(session.hasMore())
        assertNull(session.next())
        assertEquals(1, session.currentIndex)
    }

    @Test
    fun hasMoreWhileBlocksRemain() {
        val session = TextReadingSession()
        session.load(
            listOf(
                TextBlock("a", 0),
                TextBlock("b", 1),
                TextBlock("c", 2),
            ),
        )
        assertTrue(session.hasMore())
        assertEquals("b", session.next())
        assertTrue(session.hasMore())
        assertEquals("c", session.next())
        assertFalse(session.hasMore())
        assertNull(session.next())
    }

    @Test
    fun repeatReturnsCurrentWithoutAdvancing() {
        val session = TextReadingSession()
        session.load(listOf(TextBlock("only", 0)))
        assertEquals("only", session.repeat())
        assertEquals("only", session.repeat())
        assertEquals(0, session.currentIndex)
    }

    @Test
    fun stopClearsNavigation() {
        val session = TextReadingSession()
        session.load(listOf(TextBlock("a", 0)))
        session.stop()
        assertFalse(session.isReading)
        assertEquals(-1, session.currentIndex)
        assertNull(session.repeat())
        assertNull(session.next())
    }

    @Test
    fun emptyInputDoesNotStartReading() {
        val session = TextReadingSession()
        assertFalse(session.load(emptyList()))
        assertFalse(session.isReading)
    }

    @Test
    fun blankBlocksAreIgnored() {
        val session = TextReadingSession()
        assertTrue(
            session.load(
                listOf(
                    TextBlock("   ", 0),
                    TextBlock("real", 1),
                ),
            ),
        )
        assertEquals("real", session.currentBlock()?.text)
    }
}

class FakeTextRecognizerTest {

    private fun capture() = TextCapture(1, 1, ByteArray(3))

    @Test
    fun stageAndRecognizeReturnsBlocks() = runTest {
        val fake = FakeTextRecognizer()
        fake.stagePlain("hello")
        fake.load().getOrThrow()
        val result = fake.recognize(capture()).getOrThrow()
        assertEquals("hello", result.blocks.single().text)
        assertTrue(fake.isLoaded())
    }

    @Test
    fun failNextInjectsSingleFailure() = runTest {
        val fake = FakeTextRecognizer()
        fake.load().getOrThrow()
        fake.failNext()
        assertTrue(fake.recognize(capture()).isFailure)
        fake.stagePlain("ok")
        assertTrue(fake.recognize(capture()).isSuccess)
    }

    @Test
    fun recognizeBeforeLoadFails() = runTest {
        val fake = FakeTextRecognizer()
        assertTrue(fake.recognize(capture()).isFailure)
    }

    @Test
    fun unloadReleasesModel() = runTest {
        val fake = FakeTextRecognizer()
        fake.load().getOrThrow()
        fake.unload()
        assertFalse(fake.isLoaded())
        assertEquals(1, fake.unloadCallCount)
    }
}
