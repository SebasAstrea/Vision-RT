package com.visionrt.app.diagnostics

import android.content.Context
import com.visionrt.benchmark.DeviceRegistry
import com.visionrt.benchmark.detector.DetectorBenchmark
import com.visionrt.core.assistance.AssistanceController
import com.visionrt.core.common.SafeLogger
import com.visionrt.core.diagnostics.DetectorBenchmarkSummary
import com.visionrt.core.diagnostics.DiagnosticsPort
import com.visionrt.core.diagnostics.OcrBenchmarkSummary
import com.visionrt.core.ocr.OcrLifecycle
import com.visionrt.core.ocr.TextCapture
import com.visionrt.core.ocr.TextRecognizer
import com.visionrt.inference.manifest.ModelManifest
import com.visionrt.inference.manifest.ModelManifestJson
import com.visionrt.inference.runtimeapi.ModelConfig
import com.visionrt.inference.runtimeapi.ObjectDetector
import com.visionrt.perception.PerceptionFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * M3 deliverable: runs [DetectorBenchmark] against the real LiteRT model and
 * logs a payload-free report (OR-004 / NFR-PE-002). Refuses while obstacle
 * assistance is active so only one heavy model session exists.
 *
 * M5: also runs [runOcrBenchmark] against ML Kit on a synthetic page
 * (FR-009.5, budget 8 s P95).
 */
@Singleton
class DetectorBenchmarkRunner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val detector: ObjectDetector,
    private val assistanceController: AssistanceController,
    private val textRecognizer: TextRecognizer,
    private val ocrLifecycle: OcrLifecycle,
) : DiagnosticsPort {

    private val lock = Mutex()

    override suspend fun runDetectorBenchmark(): Result<DetectorBenchmarkSummary> =
        withContext(Dispatchers.Default) {
            lock.withLock {
                runCatching {
                    if (assistanceController.isActive()) {
                        error("Stop assistance before running the benchmark")
                    }
                    val owned = !detector.isLoaded()
                    try {
                        if (owned) {
                            detector.load(configOf(loadManifest())).getOrThrow()
                        }
                        val frame = syntheticFrame()
                        val report = DetectorBenchmark(
                            device = deviceLabel(),
                            warmupIterations = WARMUP,
                            sampleIterations = SAMPLES,
                        ).run {
                            runBlocking {
                                detector.detect(frame).getOrThrow()
                            }
                        }
                        val summary = DetectorBenchmarkSummary(
                            device = report.device,
                            subject = report.subject,
                            n = report.latencyMs.n,
                            p50Ms = report.latencyMs.p50Ms,
                            p95Ms = report.latencyMs.p95Ms,
                            p99Ms = report.latencyMs.p99Ms,
                            maxMs = report.latencyMs.maxMs,
                            meanMs = report.latencyMs.meanMs,
                        )
                        logReport(summary)
                        summary
                    } finally {
                        if (owned && detector.isLoaded()) {
                            detector.unload()
                        }
                    }
                }
            }
        }

    override suspend fun runOcrBenchmark(): Result<OcrBenchmarkSummary> =
        withContext(Dispatchers.Default) {
            lock.withLock {
                runCatching {
                    if (assistanceController.isActive()) {
                        error("Stop assistance before running the OCR benchmark")
                    }
                    val owned = !ocrLifecycle.isLoaded()
                    try {
                        if (owned) {
                            ocrLifecycle.ensureLoaded().getOrThrow()
                        }
                        val capture = syntheticTextCapture()
                        var lastBlocks = 0
                        val report = DetectorBenchmark(
                            subject = OCR_SUBJECT,
                            device = deviceLabel(),
                            warmupIterations = OCR_WARMUP,
                            sampleIterations = OCR_SAMPLES,
                        ).run {
                            runBlocking {
                                val recognized = textRecognizer.recognize(capture).getOrThrow()
                                lastBlocks = recognized.blocks.size
                            }
                        }
                        val summary = OcrBenchmarkSummary(
                            device = report.device,
                            subject = report.subject,
                            n = report.latencyMs.n,
                            p50Ms = report.latencyMs.p50Ms,
                            p95Ms = report.latencyMs.p95Ms,
                            p99Ms = report.latencyMs.p99Ms,
                            maxMs = report.latencyMs.maxMs,
                            meanMs = report.latencyMs.meanMs,
                            blocksFound = lastBlocks,
                        )
                        logOcrReport(summary)
                        summary
                    } finally {
                        if (owned && ocrLifecycle.isLoaded()) {
                            ocrLifecycle.unload()
                        }
                    }
                }
            }
        }

    private fun loadManifest(): ModelManifest {
        val json = context.assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() }
        return ModelManifestJson.parse(json).getOrElse { error("Invalid manifest: ${it.message}") }
    }

    private fun configOf(m: ModelManifest) = ModelConfig(
        modelId = m.modelId,
        modelVersion = m.version,
        runtime = m.runtime,
        quantization = m.quantization,
        inputWidth = m.inputWidth,
        inputHeight = m.inputHeight,
        confidenceThreshold = m.confidenceThreshold,
        nmsIouThreshold = m.nmsIouThreshold,
        maxDetections = m.maxDetections,
    )

    private fun syntheticFrame(): PerceptionFrame {
        val rgb = ByteArray(PerceptionFrame.expectedRgbSize(ANALYSIS_W, ANALYSIS_H)) { i ->
            (i % BYTE_CYCLE).toByte()
        }
        return PerceptionFrame(
            timestampMs = 0L,
            width = ANALYSIS_W,
            height = ANALYSIS_H,
            rgb888 = rgb,
        )
    }

    /** Mid-gray synthetic page; ML Kit returns empty blocks quickly (payload-free). */
    private fun syntheticTextCapture(): TextCapture {
        val w = OCR_W
        val h = OCR_H
        val gray = MID_GRAY.toByte()
        val rgb = ByteArray(w * h * RGB_BYTES) { gray }
        return TextCapture(w, h, rgb)
    }

    private fun deviceLabel(): String {
        val known = DeviceRegistry.KNOWN.firstOrNull { it.model == android.os.Build.MODEL }
        return known?.model ?: "${android.os.Build.MODEL}/${android.os.Build.HARDWARE}"
    }

    private fun logReport(s: DetectorBenchmarkSummary) {
        val pass = s.withinLatencyBudget()
        SafeLogger.i(
            TAG,
            "subject=${s.subject} device=${s.device} n=${s.n} " +
                "p50=${s.p50Ms}ms p95=${s.p95Ms}ms p99=${s.p99Ms}ms " +
                "max=${s.maxMs}ms mean=${s.meanMs}ms " +
                "budgetP95=${DetectorBenchmarkSummary.P95_BUDGET_MS}ms pass=$pass",
        )
        SafeLogger.metric(TAG, "detector_p95_ms", s.p95Ms, "ms")
        SafeLogger.metric(TAG, "detector_p99_ms", s.p99Ms, "ms")
    }

    private fun logOcrReport(s: OcrBenchmarkSummary) {
        val pass = s.withinLatencyBudget()
        SafeLogger.i(
            TAG,
            "subject=${s.subject} device=${s.device} n=${s.n} blocks=${s.blocksFound} " +
                "p50=${s.p50Ms}ms p95=${s.p95Ms}ms p99=${s.p99Ms}ms " +
                "max=${s.maxMs}ms mean=${s.meanMs}ms " +
                "budgetP95=${OcrBenchmarkSummary.OCR_P95_BUDGET_MS}ms pass=$pass",
        )
        SafeLogger.metric(TAG, "ocr_p95_ms", s.p95Ms, "ms")
    }

    companion object {
        private const val TAG = "DetectorBenchmark"
        private const val MANIFEST_ASSET = "models/manifest.json"
        private const val ANALYSIS_W = 640
        private const val ANALYSIS_H = 480
        private const val WARMUP = 5
        private const val SAMPLES = 50
        private const val BYTE_CYCLE = 251
        private const val OCR_SUBJECT = "ocr-mlkit-latin"
        private const val OCR_W = 640
        private const val OCR_H = 480
        private const val OCR_WARMUP = 2
        private const val OCR_SAMPLES = 10
        private const val MID_GRAY = 128
        private const val RGB_BYTES = 3
    }
}
