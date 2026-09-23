package com.visionrt.core.orchestration

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority

/**
 * Outbound feedback port owned by orchestration (ARCHITECTURE §9, §12).
 *
 * The concrete dispatcher lives in the `feedback` module and must implement
 * this port; orchestration (and its fakes) never touch TTS, SoundPool or
 * vibration directly.
 */
interface FeedbackPort {
    suspend fun emit(alert: Alert)

    /** Preempt in-flight feedback at or below [priority] (critical path, OR-009). */
    suspend fun interruptCurrent(priority: AlertPriority)

    suspend fun stopAll()
}
