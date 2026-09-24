package com.visionrt.core.diagnostics

/**
 * Payload-free multi-metric diagnostics snapshot for the M7 benchmark mode
 * (ARCHITECTURE §21.3). Never contains camera frames, OCR text, audio or PII.
 */
data class DiagnosticsSnapshot(
    val timestampMs: Long,
    val deviceModel: String,
    val deviceHardware: String,
    /** Registry match or null when the unit is not in DeviceRegistry. */
    val deviceInRegistry: Boolean,
    val deviceProfile: String,
    val modelId: String,
    val modelVersion: String,
    val quantization: String,
    val degradationLevel: String,
    val degradationCause: String,
    val thermalStatus: Int,
    val batteryPercent: Int,
    val batterySaverActive: Boolean,
    val memoryPeakMb: Double,
    val latencyP95Ms: Double,
    val frameIntervalMs: Long,
    val detector: DetectorBenchmarkSummary? = null,
    val ocr: OcrBenchmarkSummary? = null,
) {
    fun detectorBudgetPass(): Boolean = detector?.withinLatencyBudget() ?: false

    fun ocrBudgetPass(): Boolean = ocr?.withinLatencyBudget() ?: false

    fun memoryBudgetPass(): Boolean =
        memoryPeakMb <= MEMORY_BUDGET_MB || memoryPeakMb == 0.0

    companion object {
        /** OR-003.1 / NFR-PE-001: peak PSS ≤ 800 MB on low-end. */
        const val MEMORY_BUDGET_MB = 800.0
    }
}

/**
 * Formats a [DiagnosticsSnapshot] as stable, line-oriented text for logcat and
 * QA reports (M7 deliverables 1–5). No free-form user content is accepted.
 */
object DiagnosticsReport {
    fun format(s: DiagnosticsSnapshot): String = buildString {
        appendLine("=== VisionRT diagnostics v1 ===")
        appendLine("timestampMs=${s.timestampMs}")
        appendLine("device=${s.deviceModel} hardware=${s.deviceHardware}")
        appendLine("registry=${s.deviceInRegistry} profile=${s.deviceProfile}")
        appendLine("model=${s.modelId} version=${s.modelVersion} quant=${s.quantization}")
        appendLine(
            "level=${s.degradationLevel} cause=${s.degradationCause} " +
                "thermal=${s.thermalStatus}",
        )
        appendLine(
            "battery=${s.batteryPercent}% saver=${s.batterySaverActive} " +
                "peakMb=${s.memoryPeakMb} budgetMb=${DiagnosticsSnapshot.MEMORY_BUDGET_MB} " +
                "memPass=${s.memoryBudgetPass()}",
        )
        appendLine("p95SessionMs=${s.latencyP95Ms} intervalMs=${s.frameIntervalMs}")
        s.detector?.let {
            appendLine(
                "detector n=${it.n} p50=${it.p50Ms} p95=${it.p95Ms} " +
                    "p99=${it.p99Ms} max=${it.maxMs} mean=${it.meanMs} " +
                    "budgetPass=${it.withinLatencyBudget()}",
            )
        } ?: appendLine("detector=not-run")
        s.ocr?.let {
            appendLine(
                "ocr n=${it.n} blocks=${it.blocksFound} p50=${it.p50Ms} " +
                    "p95=${it.p95Ms} p99=${it.p99Ms} max=${it.maxMs} mean=${it.meanMs} " +
                    "budgetPass=${it.withinLatencyBudget()}",
            )
        } ?: appendLine("ocr=not-run")
        appendLine("=== end ===")
    }
}
