package com.visionrt.benchmark.detector

import com.visionrt.benchmark.core.BenchmarkReport
import com.visionrt.benchmark.core.LatencyStats

/**
 * Initial detector inference benchmark (M3 deliverable 8). Records per-inference
 * wall-clock samples and emits a [BenchmarkReport] with NFR-PE-002 percentiles.
 * Pure Kotlin: the caller supplies the timed `inferOnce` lambda so the harness
 * stays free of LiteRT/CameraX dependencies.
 */
class DetectorBenchmark(
    private val subject: String = "detector-inference-320",
    private val device: String,
    private val warmupIterations: Int = DEFAULT_WARMUP,
    private val sampleIterations: Int = DEFAULT_SAMPLES,
    private val clock: () -> Long = { System.nanoTime() / NANOS_PER_MILLI },
) {
    private val samples = mutableListOf<Long>()

    /** Clears recorded samples (does not change configuration). */
    fun reset() {
        samples.clear()
    }

    /**
     * Runs [warmupIterations] untimed warmups then [sampleIterations] timed
     * calls of [inferOnce]. Returns a report; throws if [inferOnce] throws.
     */
    fun run(inferOnce: () -> Unit): BenchmarkReport {
        samples.clear()
        repeat(warmupIterations) { inferOnce() }
        repeat(sampleIterations) {
            val start = clock()
            inferOnce()
            val end = clock()
            samples += (end - start).coerceAtLeast(0L)
        }
        return report()
    }

    fun report(): BenchmarkReport {
        require(samples.isNotEmpty()) { "No benchmark samples; call run() first" }
        return BenchmarkReport(
            subject = subject,
            latencyMs = LatencyStats.of(samples.toList()),
            device = device,
            timestampMs = System.currentTimeMillis(),
        )
    }

    fun sampleCount(): Int = samples.size

    companion object {
        const val DEFAULT_WARMUP = 5
        const val DEFAULT_SAMPLES = 50
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
