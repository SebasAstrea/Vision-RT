package com.visionrt.core.orchestration

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.domain.Detection
import com.visionrt.core.domain.Verbosity

/**
 * Runs one perception frame through [AlertPolicy] + [TemplateComposer] and
 * emits the resulting [Alert]s on the [FeedbackPort] (M2 end-to-end path).
 *
 * [Verbosity.MINIMAL] only forwards critical obstacle alerts (FR-006.6).
 * Critical obstacles interrupt lower-priority in-flight feedback before emit
 * (FR-006.3 / OR-009).
 */
class AlertPipeline(
    private val policy: AlertPolicy,
    private val feedback: FeedbackPort,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idSource: () -> Long = { clock() },
) {
    /** Clears policy cooldowns/tracks for a new assistance session. */
    fun resetPolicy() {
        policy.reset()
    }

    suspend fun onFrame(
        detections: List<Detection>,
        verbosity: Verbosity = Verbosity.NORMAL,
        motionEvidence: Boolean = false,
    ): List<Alert> {
        val candidates = policy.evaluate(detections, clock(), motionEvidence)
        val alerts = candidates
            .filter { verbosity != Verbosity.MINIMAL || it.priority == AlertPriority.CRITICAL_OBSTACLE }
            .map { candidate ->
                Alert(
                    id = "alert-${idSource()}",
                    priority = candidate.priority,
                    message = TemplateComposer.compose(candidate),
                    createdAtMs = clock(),
                )
            }
        alerts.forEach { alert ->
            if (alert.priority == AlertPriority.CRITICAL_OBSTACLE) {
                feedback.interruptCurrent(AlertPriority.USER_REQUESTED)
            }
            feedback.emit(alert)
        }
        return alerts
    }
}
