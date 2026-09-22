package com.visionrt.benchmark.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LatencyStatsTest {

    private val samples = (10L..100L step 10L).toList()

    @Test
    fun percentilesFollowCeilDefinition() {
        val stats = LatencyStats.of(samples)
        assertEquals(10, stats.n)
        assertEquals(10.0, stats.minMs, 0.0)
        assertEquals(50.0, stats.p50Ms, 0.0)
        assertEquals(100.0, stats.p95Ms, 0.0)
        assertEquals(100.0, stats.p99Ms, 0.0)
        assertEquals(100.0, stats.maxMs, 0.0)
        assertEquals(55.0, stats.meanMs, 0.0)
    }

    @Test
    fun singleSampleWorks() {
        val stats = LatencyStats.of(listOf(5L))
        assertEquals(5.0, stats.minMs, 0.0)
        assertEquals(5.0, stats.p50Ms, 0.0)
        assertEquals(5.0, stats.p95Ms, 0.0)
        assertEquals(5.0, stats.maxMs, 0.0)
    }

    @Test
    fun emptySamplesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { LatencyStats.of(emptyList()) }
    }

    @Test
    fun unsortedInputIsSortedInternally() {
        val stats = LatencyStats.of(samples.reversed())
        assertEquals(10.0, stats.minMs, 0.0)
        assertEquals(100.0, stats.maxMs, 0.0)
    }
}
