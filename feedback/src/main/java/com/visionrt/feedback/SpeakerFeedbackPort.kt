package com.visionrt.feedback

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.speech.Speaker
import com.visionrt.core.orchestration.FeedbackPort

/**
 * Minimal production [FeedbackPort] for M3: speaks alert messages through the
 * injected [Speaker]. Priority-queue and haptics land in M4 (ROADMAP).
 *
 * Critical obstacles flush the current utterance so the new phrase is heard
 * next (FR-006.3).
 */
class SpeakerFeedbackPort(
    private val speaker: Speaker,
) : FeedbackPort {

    override suspend fun emit(alert: Alert) {
        if (alert.message.isBlank()) return
        speaker.speak(alert.message)
    }

    override suspend fun interruptCurrent(priority: AlertPriority) {
        if (priority == AlertPriority.CRITICAL_OBSTACLE) {
            speaker.stop()
        }
    }

    override suspend fun stopAll() {
        speaker.stop()
    }
}
