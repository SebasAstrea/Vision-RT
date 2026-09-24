package com.visionrt.app.resource

import com.visionrt.app.memory.AndroidMemoryMonitor
import com.visionrt.core.common.SafeLogger
import com.visionrt.core.domain.DeviceProfileClassifier
import com.visionrt.core.domain.DeviceProfileProvider
import com.visionrt.core.orchestration.FeedbackPort
import com.visionrt.core.orchestration.ModeController
import com.visionrt.core.orchestration.OrchestrationState
import com.visionrt.core.resource.DegradationAnnouncer
import com.visionrt.core.resource.DegradationLevel
import com.visionrt.core.resource.ResourceCause
import com.visionrt.core.resource.ResourceGovernor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Samples device signals, runs [ResourceGovernor], announces degradation
 * (OR-006.2.2 / OR-010.5 / FR-015.4) and logs diagnostics events (M6 D8).
 * Frame interval is dynamic for the obstacle loop (OR-005 / OR-006.1).
 * Critical level maps to ModeController DEGRADED (ARCHITECTURE §9.4 L3).
 */
@Singleton
class ResourceManager @Inject constructor(
    private val signals: AndroidResourceSignals,
    private val memoryMonitor: AndroidMemoryMonitor,
    deviceProfileProvider: DeviceProfileProvider,
    private val feedback: FeedbackPort,
    private val modeController: ModeController,
) {
    private val baseIntervalMs: Long =
        DeviceProfileClassifier.frameIntervalMs(deviceProfileProvider.profile)

    val governor = ResourceGovernor(baseIntervalMs)
    private val announcer = DegradationAnnouncer(feedback)

    private val _level = MutableStateFlow(governor.level)
    val level: StateFlow<DegradationLevel> = _level.asStateFlow()

    /** Recent inference latencies for OR-004.6 / OR-010.1 input. */
    private val latencySamplesMs = ArrayDeque<Double>(MAX_LATENCY_SAMPLES)

    fun onSessionStart() {
        governor.reset()
        announcer.reset()
        latencySamplesMs.clear()
        _level.value = governor.level
        SafeLogger.i(TAG, "session start baseIntervalMs=$baseIntervalMs")
    }

    fun onSessionStop() {
        SafeLogger.i(TAG, governorSummary())
        governor.reset()
        announcer.reset()
        _level.value = governor.level
    }

    fun recordInferenceLatencyMs(ms: Double) {
        if (ms <= 0.0) return
        latencySamplesMs.addLast(ms)
        while (latencySamplesMs.size > MAX_LATENCY_SAMPLES) latencySamplesMs.removeFirst()
    }

    /**
     * One evaluation tick. Returns the frame delay to use next, or null when
     * continuous detection must stop (critical thermal / camera lost).
     */
    suspend fun tick(cameraAvailable: Boolean = true): Long? {
        val peak = memoryMonitor.budget.peakMb
        val before = governor.level
        val beforeCause = governor.lastCause
        val next = governor.evaluate(
            signals.sample(
                memoryPeakMb = peak,
                latencyP95Ms = latencyP95Ms(),
                cameraAvailable = cameraAvailable,
            ),
        )
        if (next != before || governor.lastCause != beforeCause) {
            logTransition(before, next, governor.lastCause, peak)
        }
        _level.value = next
        if (announcer.onGovernorState(next, governor.lastCause)) {
            SafeLogger.i(
                TAG,
                "announced level=${next.name} cause=${governor.lastCause.name}",
            )
        }
        syncModeWithLevel(next)
        return if (governor.continuousDetectionStopped()) {
            null
        } else {
            governor.frameIntervalMs()
        }
    }

    /** DEGRADED ↔ OBSTACLE_ASSISTANCE_ACTIVE follows the ladder (§8.2). */
    private suspend fun syncModeWithLevel(level: DegradationLevel) {
        if (level == DegradationLevel.CRITICAL &&
            modeController.current == OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE
        ) {
            modeController.degrade()
            SafeLogger.i(TAG, "mode -> DEGRADED (critical resource level)")
        } else if (level != DegradationLevel.CRITICAL &&
            modeController.current == OrchestrationState.DEGRADED
        ) {
            modeController.returnToObstacleAssistance()
            SafeLogger.i(TAG, "mode -> OBSTACLE_ASSISTANCE_ACTIVE (recovered)")
        }
    }

    private fun latencyP95Ms(): Double {
        if (latencySamplesMs.isEmpty()) return 0.0
        val sorted = latencySamplesMs.sorted()
        val idx = ((sorted.size - 1) * P95_NUM) / P95_DEN
        return sorted[idx.toInt()]
    }

    private fun logTransition(
        from: DegradationLevel,
        to: DegradationLevel,
        cause: ResourceCause,
        peakMb: Double,
    ) {
        val thermal = signals.sample().thermalStatus
        SafeLogger.w(
            TAG,
            "degradation $from -> $to cause=${cause.name} " +
                "thermal=$thermal peakMb=$peakMb " +
                "intervalMs=${governor.frameIntervalMs()}",
        )
    }

    fun governorSummary(): String {
        val s = signals.sample(memoryPeakMb = memoryMonitor.budget.peakMb)
        return "level=${governor.level.name} cause=${governor.lastCause.name} " +
            "thermal=${s.thermalStatus} saver=${s.batterySaverActive} " +
            "battery=${s.batteryPercent}% peakMb=${s.memoryPeakMb} " +
            "p95Ms=${latencyP95Ms()} intervalMs=${governor.frameIntervalMs()}"
    }

    /** Session inference P95 for M7 diagnostics export (§21.3); 0 when empty. */
    fun diagnosticsLatencyP95Ms(): Double = latencyP95Ms()

    private companion object {
        const val TAG = "ResourceManager"
        const val MAX_LATENCY_SAMPLES = 60
        const val P95_NUM = 95
        const val P95_DEN = 100
    }
}
