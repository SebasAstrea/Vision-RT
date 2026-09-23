package com.visionrt.perception.framegate

import com.visionrt.perception.PerceptionFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LatestOnlyFrameGateTest {

    private fun frame(id: Long) = PerceptionFrame(timestampMs = id, width = 640, height = 480)

    @Test
    fun submitThenLatestReturnsFrame() {
        val gate = LatestOnlyFrameGate()
        val f = frame(1)
        gate.submit(f)
        assertSame(f, gate.latest())
    }

    @Test
    fun submitReplacesUnreadFrame() {
        val gate = LatestOnlyFrameGate()
        val first = frame(1)
        val second = frame(2)
        gate.submit(first)
        gate.submit(second)
        assertSame(second, gate.latest())
    }

    @Test
    fun consumeClearsGate() {
        val gate = LatestOnlyFrameGate()
        gate.submit(frame(1))
        val taken = gate.consume()
        assertEquals(1L, taken?.timestampMs)
        assertNull(gate.latest())
        assertNull(gate.consume())
    }

    @Test
    fun clearEmptiesGate() {
        val gate = LatestOnlyFrameGate()
        gate.submit(frame(7))
        gate.clear()
        assertNull(gate.latest())
    }

    @Test
    fun latestDoesNotConsume() {
        val gate = LatestOnlyFrameGate()
        val f = frame(3)
        gate.submit(f)
        assertSame(f, gate.latest())
        assertSame(f, gate.latest())
    }
}
