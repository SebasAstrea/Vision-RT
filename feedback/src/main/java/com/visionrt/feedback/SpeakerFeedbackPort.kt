package com.visionrt.feedback

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.orchestration.FeedbackPort
import com.visionrt.core.speech.Speaker
import java.util.concurrent.atomic.AtomicReference

/**
 * Production [FeedbackPort]: speaks alert messages through [Speaker].
 *
 * Tracks the priority of the in-flight utterance so [AlertPipeline] can avoid
 * interrupting a CRITICAL alert with another CRITICAL alert (which would cut
 * the phrase mid-word and produce audio "noise"). Lower-priority alerts are
 * still preempted by critical ones (FR-006.3 / OR-009).
 */
class SpeakerFeedbackPort(
    private val speaker: Speaker,
) : FeedbackPort {

    private val inFlightPriority = AtomicReference<AlertPriority?>(null)

    override suspend fun emit(alert: Alert) {
        if (alert.message.isBlank()) return
            inFlightPriority.set(alert.priority)
            try {
                speaker.speak(alert.message)
            } finally {
                // Clear only if still ours (another emit might have replaced it).
                inFlightPriority.compareAndSet(alert.priority, null)
            }
    }

    override suspend fun interruptCurrent(priority: AlertPriority) {
        // Only stop if what is in flight is at or below the requested priority.
        val current = inFlightPriority.get() ?: return
        if (current.ordinal <= priority.ordinal) {
            speaker.stop()
            inFlightPriority.set(null)
        }
    }

    override suspend fun stopAll() {
        speaker.stop()
        inFlightPriority.set(null)
    }

    override fun currentPriority(): AlertPriority? = inFlightPriority.get()

    override fun isActive(): Boolean = inFlightPriority.get() != null
}
