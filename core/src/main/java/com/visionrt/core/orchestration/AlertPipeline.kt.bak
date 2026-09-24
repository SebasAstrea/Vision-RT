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
 * [Verbosity.DETAILED] uses a shorter cooldown and may emit longer phrases
 * (FR-006.2/5). Critical obstacles interrupt lower-priority in-flight feedback
 * before emit (FR-006.3 / OR-009).
 */
class AlertPipeline(
    private val policy: AlertPolicy,
    private val feedback: FeedbackPort,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idSource: () -> Long = { clock() },
    private val lang: AlertLang = AlertLang.current(),
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
        val candidates = policy.evaluate(detections, clock(), motionEvidence, verbosity)
        val alerts = candidates
        .filter { verbosity != Verbosity.MINIMAL || it.priority == AlertPriority.CRITICAL_OBSTACLE }
        .map { candidate ->
            Alert(
                id = "alert-${idSource()}",
                  priority = candidate.priority,
                  message = TemplateComposer.compose(candidate, lang),
                  createdAtMs = clock(),
                  sector = candidate.detection.sector,
                  proximity = candidate.detection.proximity,
            )
        }

        alerts.forEach { alert ->
            val current = feedback.currentPriority()
            // Interrupt only when the new alert is strictly more severe than the
            // one currently in flight. Equal priority means "let the current phrase
            // finish"; the cooldown will gate the next emission.
            if (current != null && alert.priority.ordinal > current.ordinal) {
                feedback.interruptCurrent(alert.priority)
            }
            feedback.emit(alert)
        }
        return alerts
    }
