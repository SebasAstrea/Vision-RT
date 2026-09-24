package com.visionrt.app.ocr

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.visionrt.core.common.SafeLogger
import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.ocr.ObjectSummaryComposer
import com.visionrt.core.ocr.RecognizerOcrSource
import com.visionrt.core.ocr.TextCapture
import com.visionrt.core.ocr.TextReadingService
import com.visionrt.core.ocr.TextReadingSession
import com.visionrt.core.orchestration.AlertLang
import com.visionrt.core.orchestration.FeedbackPort
import com.visionrt.core.orchestration.ModeController
import com.visionrt.core.orchestration.OrchestrationState
import com.visionrt.core.summary.ObjectSummaryService
import com.visionrt.data.settings.SettingsRepository
import com.visionrt.inference.manifest.ModelManifestJson
import com.visionrt.inference.runtimeapi.ModelConfig
import com.visionrt.inference.runtimeapi.ObjectDetector
import com.visionrt.perception.PerceptionFrame
import com.visionrt.perception.camera.CameraFrameSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * M5 coordinators (app module, ARCHITECTURE §7.1.3 / §18.2–18.3).
 *
 * Object summary: one-shot detect on the latest analysis frame (FR-008).
 * Text reading: load OCR, recognize, navigate blocks (FR-009); OCR unloads
 * via [OcrLifecycleManager] after 30 s idle (OR-002.7).
 */
@Singleton
@Suppress("LongParameterList", "TooManyFunctions") // mode wiring (ARCHITECTURE §7.1.3)
class AssistanceModesCoordinator @Inject constructor(
    private val modeController: ModeController,
    private val detector: ObjectDetector,
    private val camera: CameraFrameSource,
    private val feedback: FeedbackPort,
    private val settings: SettingsRepository,
    private val ocrSource: RecognizerOcrSource,
    private val ocrLifecycle: OcrLifecycleManager,
    @param:ApplicationContext private val context: Context,
) : ObjectSummaryService, TextReadingService {

    private val readingSession = TextReadingSession()
    private val lang: AlertLang get() = AlertLang.current()

    override suspend fun describeScene(): Result<String> = withContext(Dispatchers.Default) {
        val wasObstacle = modeController.current == OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE
        runCatching {
            if (!wasObstacle) {
                check(modeController.enterObjectQuery()) {
                    "Cannot enter object query from ${modeController.current}"
                }
            }
            val summary = withTimeoutOrNull(SUMMARY_TIMEOUT_MS) {
                ensureCameraAndDetector()
                val detections = runCatching {
                    val frame = camera.consumeLatestFrame() ?: camera.peekLatestFrame()
                    if (frame == null || !detector.isLoaded()) emptyList()
                    else detector.detect(frame).getOrThrow()
                }.getOrElse { emptyList() }
                ObjectSummaryComposer.compose(detections, lang)
            } ?: ObjectSummaryComposer.timeoutMessage(lang)
            emitStatus(summary, AlertPriority.USER_REQUESTED)
            if (!wasObstacle && modeController.current == OrchestrationState.OBJECT_QUERY_ACTIVE) {
                modeController.stopSession()
            }
            summary
        }.recoverCatching { t ->
            SafeLogger.w(TAG, "Object summary failed: ${t.javaClass.simpleName}")
            emitStatus(ObjectSummaryComposer.failureMessage(lang), AlertPriority.STATUS)
            if (!wasObstacle && modeController.current == OrchestrationState.OBJECT_QUERY_ACTIVE) {
                modeController.stopSession()
            }
            throw t
        }
    }

    override suspend fun startReading(): Result<String> = withContext(Dispatchers.Default) {
        val wasObstacle = modeController.current == OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE
        runCatching {
            if (modeController.current != OrchestrationState.TEXT_READING_ACTIVE) {
                check(modeController.enterTextReading()) {
                    "Cannot enter text reading from ${modeController.current}"
                }
            }
            ensureCameraAndDetector()
            ocrSource.updateCaptureProvider {
                val frame: PerceptionFrame? = camera.peekLatestFrame() ?: camera.consumeLatestFrame()
                val rgb = frame?.rgb888
                if (frame != null && rgb != null) {
                    TextCapture(frame.width, frame.height, rgb)
                } else {
                    null
                }
            }
            ocrLifecycle.ensureLoaded().getOrThrow()
            val recognized = withTimeoutOrNull(OCR_TIMEOUT_MS) {
                ocrSource.recognizeBlocks().getOrThrow()
            } ?: error("ocr-timeout")
            if (recognized.isEmpty) error("empty-ocr")
            readingSession.load(recognized.blocks)
            val first = readingSession.repeat() ?: error("empty-ocr")
            speakReading(first)
            first
        }.recoverCatching { t ->
            SafeLogger.w(TAG, "Text reading start failed: ${t.javaClass.simpleName}")
            emitStatus(TextReadingSession.unclearMessage(lang), AlertPriority.OCR_RESULT)
            readingSession.stop()
            if (modeController.current == OrchestrationState.TEXT_READING_ACTIVE) {
                if (wasObstacle) modeController.returnToObstacleAssistance()
                else modeController.stopSession()
            }
            throw t
        }
    }

    override suspend fun repeat(): Result<String> = runCatching {
        val text = readingSession.repeat() ?: error("not-reading")
        speakReading(text)
        text
    }

    override suspend fun next(): Result<String> = runCatching {
        val text = readingSession.next()
        if (text == null) {
            speakReading(TextReadingSession.endMessage(lang))
            error(TextReadingService.END_OF_TEXT)
        } else {
            speakReading(text)
            text
        }
    }

    override suspend fun stopReading() {
        readingSession.stop()
        feedback.stopAll()
        ocrLifecycle.touch()
        if (modeController.current == OrchestrationState.TEXT_READING_ACTIVE) {
            if (modeController.canReturnToObstacle()) {
                modeController.returnToObstacleAssistance()
            } else {
                modeController.stopSession()
            }
        }
    }

    override fun isActive(): Boolean = readingSession.isReading

    override fun blockCount(): Int = readingSession.blocks.size

    private suspend fun ensureCameraAndDetector() {
        if (!detector.isLoaded()) {
            val manifest = withContext(Dispatchers.IO) {
                context.assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() }
                    .let { ModelManifestJson.parse(it).getOrThrow() }
            }
            detector.load(
                ModelConfig(
                    modelId = manifest.modelId,
                    modelVersion = manifest.version,
                    runtime = manifest.runtime,
                    quantization = manifest.quantization,
                    inputWidth = manifest.inputWidth,
                    inputHeight = manifest.inputHeight,
                    confidenceThreshold = manifest.confidenceThreshold,
                    nmsIouThreshold = manifest.nmsIouThreshold,
                    maxDetections = manifest.maxDetections,
                ),
            ).getOrThrow()
        }
        if (!camera.isBound) {
            val owner: LifecycleOwner = ProcessLifecycleOwner.get()
            camera.bind(owner).getOrThrow()
        }
    }

    private suspend fun emitStatus(message: String, priority: AlertPriority) {
        feedback.emit(
            Alert(
                id = "modes",
                priority = priority,
                message = message,
                createdAtMs = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun speakReading(text: String) {
        if (!settings.speechMuted.first()) {
            feedback.emit(
                Alert(
                    id = "ocr-block",
                    priority = AlertPriority.OCR_RESULT,
                    message = text,
                    createdAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private companion object {
        const val TAG = "AssistanceModes"
        const val MANIFEST_ASSET = "models/manifest.json"
        const val SUMMARY_TIMEOUT_MS = 8_000L
        const val OCR_TIMEOUT_MS = 8_000L
    }
}
