package com.visionrt.app.assistance

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.visionrt.app.memory.AndroidMemoryMonitor
import com.visionrt.core.assistance.AssistanceController
import com.visionrt.core.common.SafeLogger
import com.visionrt.core.domain.DeviceProfile
import com.visionrt.core.domain.DeviceProfileClassifier
import com.visionrt.core.domain.DeviceProfileProvider
import com.visionrt.core.domain.Detection
import com.visionrt.core.orchestration.AlertPipeline
import com.visionrt.core.orchestration.DetectionSource
import com.visionrt.core.orchestration.ModeController
import com.visionrt.core.orchestration.OrchestrationState
import com.visionrt.data.settings.SettingsRepository
import com.visionrt.inference.manifest.ModelManifestJson
import com.visionrt.inference.runtimeapi.ModelConfig
import com.visionrt.inference.runtimeapi.ObjectDetector
import com.visionrt.perception.camera.CameraFrameSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * M3 obstacle-assistance wiring (app module, ARCHITECTURE §7.1.3).
 *
 * Camera permission must be granted before [start]. Frame interval follows the
 * device profile (OR-001.3c). Verbosity comes from user settings (FR-006.5).
 * PSS is sampled while active (OR-003).
 */
@Singleton
@Suppress("LongParameterList") // app-level wiring (ARCHITECTURE §7.1.3)
class ObstacleAssistanceCoordinator @Inject constructor(
    private val modeController: ModeController,
    private val detector: ObjectDetector,
    private val camera: CameraFrameSource,
    private val alertPipeline: AlertPipeline,
    private val detectionSource: DetectionSource,
    private val deviceProfileProvider: DeviceProfileProvider,
    private val memoryMonitor: AndroidMemoryMonitor,
    private val settings: SettingsRepository,
    @param:ApplicationContext private val context: Context,
) : AssistanceController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private val frameIntervalMs: Long =
        DeviceProfileClassifier.frameIntervalMs(deviceProfileProvider.profile)

    val state: OrchestrationState get() = modeController.current
    val profile: DeviceProfile get() = deviceProfileProvider.profile

    override fun isActive(): Boolean =
        modeController.current == OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE

    override suspend fun start(): Result<Unit> = runCatching {
        if (isActive()) return Result.success(Unit)
        val started = modeController.start()
        if (!started) error("Cannot start from ${modeController.current}")

        val manifest = validateManifest()
        detector.load(configOf(manifest)).getOrThrow()
        val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
        camera.bind(lifecycleOwner).getOrThrow()
        alertPipeline.resetPolicy()
        memoryMonitor.startSession()

        val ready = modeController.onReady()
        if (!ready) error("Could not enter OBSTACLE_ASSISTANCE_ACTIVE")

        loopJob = scope.launch { inferenceLoop() }
        SafeLogger.i(
            TAG,
            "Obstacle assistance started profile=${deviceProfileProvider.profile.key} " +
                "intervalMs=$frameIntervalMs",
        )
    }

    override suspend fun stop(): Result<Unit> = runCatching {
        loopJob?.cancelAndJoin()
        loopJob = null
        camera.unbind()
        camera.clearFrames()
        detector.unload()
        modeController.stopSession()
        memoryMonitor.stopSessionAndLog()
        SafeLogger.i(TAG, "Obstacle assistance stopped")
    }

    private suspend fun validateManifest() = withContext(Dispatchers.IO) {
        val json = context.assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() }
        ModelManifestJson.parse(json).getOrThrow()
    }

    private fun configOf(m: com.visionrt.inference.manifest.ModelManifest) = ModelConfig(
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

    private suspend fun inferenceLoop() {
        var lastMemorySampleMs = 0L
        while (true) {
            val frame = frameOrNull()
            if (frame != null) {
                val detections = detectionSource.detect().getOrElse { emptyList() }
                emitAlerts(detections)
                sampleMemoryIfDue(lastMemorySampleMs)
                lastMemorySampleMs = System.currentTimeMillis()
                delay(frameIntervalMs)
            } else {
                delay(IDLE_POLL_MS)
            }
        }
    }

    private fun sampleMemoryIfDue(lastSampleMs: Long) {
        val now = System.currentTimeMillis()
        if (now - lastSampleMs >= MEMORY_SAMPLE_INTERVAL_MS) {
            memoryMonitor.sampleNow()
        }
    }

    private fun frameOrNull() =
        if (modeController.current == OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE) {
            camera.peekLatestFrame()
        } else {
            null
        }

    private suspend fun emitAlerts(detections: List<Detection>) {
        runCatching {
            val verbosity = settings.verbosity.first()
            alertPipeline.onFrame(detections, verbosity, motionEvidence = false)
        }.onFailure { t ->
            SafeLogger.w(TAG, "Alert pipeline failed: ${t.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "AssistanceCoordinator"
        const val MANIFEST_ASSET = "models/manifest.json"
        const val IDLE_POLL_MS = 50L
        const val MEMORY_SAMPLE_INTERVAL_MS = 5_000L
    }
}
