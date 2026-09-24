package com.visionrt.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryBudgetMonitorTest {

    @Test
    fun firstSampleSetsBaselineAndTracksPeak() {
        val m = MemoryBudgetMonitor()
        m.sample(400.0, timestampMs = 1)
        m.sample(420.0, timestampMs = 2)
        m.sample(390.0, timestampMs = 3)
        assertEquals(400.0, m.baseline()!!, 0.0)
        assertEquals(420.0, m.peakMb, 0.0)
        assertEquals(3, m.sampleCount)
    }

    @Test
    fun peakOverEightHundredFailsBudget() {
        val m = MemoryBudgetMonitor()
        m.sample(700.0)
        m.sample(810.0)
        assertFalse(m.peakWithinBudget())
        assertEquals(MemoryPressureLevel.CRITICAL, m.pressureLevel())
    }

    @Test
    fun warnLevelBetween650And750() {
        val m = MemoryBudgetMonitor()
        m.sample(500.0)
        m.sample(700.0)
        assertTrue(m.peakWithinBudget())
        assertEquals(MemoryPressureLevel.WARN, m.pressureLevel())
    }

    @Test
    fun growthOverTenPercentFails() {
        val m = MemoryBudgetMonitor()
        m.sample(400.0, timestampMs = 0)
        m.sample(450.0, timestampMs = 1000)
        assertEquals(12.5, m.peakGrowthPct()!!, 0.001)
        assertFalse(m.growthWithinBudget())
    }

    @Test
    fun growthWithinBudgetWhenStable() {
        val m = MemoryBudgetMonitor()
        m.sample(400.0, timestampMs = 0)
        m.sample(430.0, timestampMs = 1000)
        assertTrue(m.growthWithinBudget())
        assertTrue(m.peakWithinBudget())
        assertEquals(MemoryPressureLevel.OK, m.pressureLevel())
    }

    @Test
    fun lowMemorySignalsAreCounted() {
        val m = MemoryBudgetMonitor()
        m.onLowMemorySignal()
        m.onLowMemorySignal()
        assertEquals(2, m.lowMemoryEventCount)
    }

    @Test
    fun resetClearsSession() {
        val m = MemoryBudgetMonitor()
        m.sample(500.0)
        m.onLowMemorySignal()
        m.reset()
        assertEquals(0, m.sampleCount)
        assertEquals(0.0, m.peakMb, 0.0)
        assertNull(m.baseline())
        assertEquals(0, m.lowMemoryEventCount)
    }

    @Test
    fun busPublishesToRegisteredListeners() {
        val bus = MemoryPressureBus()
        val seen = mutableListOf<Int>()
        val first = MemoryPressureBus.Listener { level -> seen += level }
        val second = MemoryPressureBus.Listener { level -> seen += level + 100 }
        bus.add(first)
        bus.add(second)
        bus.publish(5)
        assertEquals(listOf(5, 105), seen)
        assertEquals(2, bus.listenerCount())
        bus.remove(first)
        bus.publish(6)
        assertEquals(listOf(5, 105, 106), seen)
        assertEquals(1, bus.listenerCount())
    }
}
