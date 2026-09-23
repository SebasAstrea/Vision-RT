package com.visionrt.feedback

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.orchestration.FeedbackPort

/**
 * Output channel abstraction (ARCHITECTURE §12). Implementations play TTS,
 * earcons and haptics; the priority queue honours [AlertPriority] ordering and
 * is injected here by the app. No caller outside feedback/ may touch TTS,
 * SoundPool or Vibration directly.
 *
 * Extends [FeedbackPort] so orchestration (core) can depend only on the port.
 */
interface FeedbackDispatcher : FeedbackPort {
    override suspend fun emit(alert: Alert)

    override suspend fun interruptCurrent(priority: AlertPriority)

    override suspend fun stopAll()
}
