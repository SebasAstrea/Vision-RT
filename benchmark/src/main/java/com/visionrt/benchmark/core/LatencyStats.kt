package com.visionrt.benchmark.core

import kotlin.math.ceil

/**
 * Latency distribution for one benchmark run. Pure Kotlin (JVM-testable).
 * Percentiles use the standard ceil(p/100 * n) - 1 definition so the harness
 * later reports the exact budgets from NFR-PE-002 (P95/P99).
 */
data class LatencyStats(
    val n: Int,
    val minMs: Double,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
    val meanMs: Double,
) {
    companion object {
        private const val PERCENTILE_50 = 50.0
        private const val PERCENTILE_95 = 95.0
        private const val PERCENTILE_99 = 99.0
        private const val PERCENT_SCALE = 100.0

        fun of(samplesMs: List<Long>): LatencyStats {
            require(samplesMs.isNotEmpty()) { "No latency samples" }
            val sorted = samplesMs.sorted()
            val n = sorted.size

            fun percentile(p: Double): Double {
                val index = ceil(p * n / PERCENT_SCALE).toInt().coerceIn(1, n) - 1
                return sorted[index].toDouble()
            }

            return LatencyStats(
                n = n,
                minMs = sorted.first().toDouble(),
                p50Ms = percentile(PERCENTILE_50),
                p95Ms = percentile(PERCENTILE_95),
                p99Ms = percentile(PERCENTILE_99),
                maxMs = sorted.last().toDouble(),
                meanMs = sorted.sum().toDouble() / n,
            )
        }
    }
}
