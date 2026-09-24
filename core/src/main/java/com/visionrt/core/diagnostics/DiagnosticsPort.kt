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
 * OCR end-to-end benchmark summary (M5 deliverable 8 / FR-009.5).
 * Budget is the FR-009.5 P95 ≤ 8000 ms on a standard single-page capture.
 */
data class OcrBenchmarkSummary(
    val device: String,
    val subject: String,
    val n: Int,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
    val meanMs: Double,
    val blocksFound: Int,
) {
    fun withinLatencyBudget(): Boolean = p95Ms <= OCR_P95_BUDGET_MS

    companion object {
        const val OCR_P95_BUDGET_MS = 8_000.0
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

    /**
     * Runs a one-shot OCR benchmark (FR-009.5). Refuses while assistance or
     * text reading is active so only one heavy model session exists.
     */
    suspend fun runOcrBenchmark(): Result<OcrBenchmarkSummary>

    /**
     * Captures the M7 multi-metric diagnostics snapshot (ARCHITECTURE §21.3):
     * device profile, model metadata, degradation/memory/thermal state and
     * the last detector/OCR benchmark results if present. Payload-free.
     */
    suspend fun collectDiagnosticsSnapshot(): Result<DiagnosticsSnapshot>
}
