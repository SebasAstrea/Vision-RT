package com.visionrt.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportTest {

    private fun detector(
        p95: Double = 400.0,
        n: Int = 50,
    ) = DetectorBenchmarkSummary(
        device = "SM-A226BR",
        subject = "detector-int8",
        n = n,
        p50Ms = p95 - 80.0,
        p95Ms = p95,
        p99Ms = p95 + 40.0,
        maxMs = p95 + 60.0,
        meanMs = p95 - 100.0,
    )

    private fun snapshot(
        peakMb: Double = 420.0,
        detector: DetectorBenchmarkSummary? = detector(),
    ) = DiagnosticsSnapshot(
        timestampMs = 1_700_000_000_000L,
        deviceModel = "SM-A226BR",
        deviceHardware = "mt6833",
        deviceInRegistry = true,
        deviceProfile = "LOW_END",
        modelId = "yolov8n-320",
        modelVersion = "1.0.0",
        quantization = "int8",
        degradationLevel = "NORMAL",
        degradationCause = "NONE",
        thermalStatus = 0,
        batteryPercent = 88,
        batterySaverActive = false,
        memoryPeakMb = peakMb,
        latencyP95Ms = 390.0,
        frameIntervalMs = 66L,
        detector = detector,
        ocr = null,
    )

    @Test
    fun formatIncludesCoreCountersWithoutFreeFormPayload() {
        val text = DiagnosticsReport.format(snapshot())
        assertTrue(text.contains("device=SM-A226BR"))
        assertTrue(text.contains("profile=LOW_END"))
        assertTrue(text.contains("quant=int8"))
        assertTrue(text.contains("level=NORMAL"))
        assertTrue(text.contains("budgetPass=true"))
        assertTrue(text.contains("ocr=not-run"))
        assertFalse(text.contains("ocrText="))
        assertFalse(text.contains("frame="))
    }

    @Test
    fun formatMarksMissingBenchmarks() {
        val text = DiagnosticsReport.format(snapshot(detector = null))
        assertTrue(text.contains("detector=not-run"))
    }

    @Test
    fun memoryBudgetPassAt800AndBelow() {
        assertTrue(snapshot(peakMb = 800.0).memoryBudgetPass())
        assertTrue(snapshot(peakMb = 0.0).memoryBudgetPass())
        assertFalse(snapshot(peakMb = 801.0).memoryBudgetPass())
    }

    @Test
    fun detectorBudgetHelpersFollowLatencyBudget() {
        assertTrue(snapshot(detector = detector(p95 = 450.0)).detectorBudgetPass())
        assertFalse(snapshot(detector = detector(p95 = 451.0)).detectorBudgetPass())
        assertFalse(snapshot(detector = null).detectorBudgetPass())
    }
}
