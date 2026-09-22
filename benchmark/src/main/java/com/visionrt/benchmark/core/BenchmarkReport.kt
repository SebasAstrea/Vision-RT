package com.visionrt.benchmark.core

/**
 * Result of one benchmark subject run. The device field carries the physical
 * instrument (see DeviceRegistry) so thresholds are interpreted per hardware.
 */
data class BenchmarkReport(
    val subject: String,
    val latencyMs: LatencyStats,
    val device: String,
    val timestampMs: Long,
)
