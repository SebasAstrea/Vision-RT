package com.visionrt.core.diagnostics

/**
 * Detector inference benchmark summary (M3 deliverable 8 / OR-004).
 * Shaped for spoken/announced UI results; thresholds from NFR-PE-002.
 */
data class DetectorBenchmarkSummary(
    val device: String,
    val subject: String,
    val n: Int,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
    val meanMs: Double,
) {
    /** NFR-PE-002: detector inference P95 ≤ 450 ms at 320×320. */
    fun withinLatencyBudget(): Boolean = p95Ms <= P95_BUDGET_MS

    companion object {
        const val P95_BUDGET_MS = 450.0
    }
}

/**
 * App-facing diagnostics port (feature → core only; benchmark/inference stay
 * behind the app module — ARCHITECTURE §7.1 allowlist).
 */
interface DiagnosticsPort {
    /**
     * Runs a one-shot on-device detector benchmark (warmup + timed samples).
     * Fails if assistance is active or the model cannot load.
     */
    suspend fun runDetectorBenchmark(): Result<DetectorBenchmarkSummary>
}
