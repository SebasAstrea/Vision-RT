package com.visionrt.feedback

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.speech.Speaker
import com.visionrt.core.orchestration.FeedbackPort

/**
 * Minimal production [FeedbackPort] for M3: speaks alert messages through the
 * injected [Speaker]. Priority-queue and haptics land in M4 (ROADMAP).
 */
class SpeakerFeedbackPort(
    private val speaker: Speaker,
) : FeedbackPort {

    override suspend fun emit(alert: Alert) {
        if (alert.message.isBlank()) return
        speaker.speak(alert.message)
    }

    override suspend fun interruptCurrent(priority: AlertPriority) {
        // M3: flush current utterance so a critical alert is heard next.
        if (priority == AlertPriority.CRITICAL_OBSTACLE) {
            speaker.stop()
        }
    }

    override suspend fun stopAll() {
        speaker.stop()
    }
}
