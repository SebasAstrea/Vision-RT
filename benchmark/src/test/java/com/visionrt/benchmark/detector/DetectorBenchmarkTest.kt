package com.visionrt.benchmark.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorBenchmarkTest {

    @Test
    fun runCollectsConfiguredSampleCount() {
        var ticks = 0L
        val benchmark = DetectorBenchmark(
            device = "test",
            warmupIterations = 2,
            sampleIterations = 10,
            clock = { ++ticks },
        )
        val report = benchmark.run { /* timed body */ }
        assertEquals(10, benchmark.sampleCount())
        assertEquals(10, report.latencyMs.n)
        assertTrue(report.subject.contains("detector"))
        assertTrue(report.device == "test")
    }

    @Test
    fun warmupDoesNotContributeSamples() {
        var ticks = 0L
        val benchmark = DetectorBenchmark(
            device = "test",
            warmupIterations = 7,
            sampleIterations = 3,
            clock = { ++ticks },
        )
        benchmark.run { }
        assertEquals(3, benchmark.sampleCount())
        // clock() is only sampled around timed bodies: 3 samples × 2 reads.
        assertEquals(6L, ticks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun reportWithoutSamplesFails() {
        DetectorBenchmark(device = "test").report()
    }

    @Test
    fun resetClearsSamples() {
        val benchmark = DetectorBenchmark(device = "test", warmupIterations = 0, sampleIterations = 5)
        var t = 0L
        benchmark.run { t++ }
        assertEquals(5, benchmark.sampleCount())
        benchmark.reset()
        assertEquals(0, benchmark.sampleCount())
    }
}
