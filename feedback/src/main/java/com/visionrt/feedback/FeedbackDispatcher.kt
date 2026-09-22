package com.visionrt.feedback

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority

/**
 * Output channel abstraction (ARCHITECTURE §12). Implementations play TTS,
 * earcons and haptics; the priority queue honours [AlertPriority] ordering and
 * is injected here by the app. No caller outside feedback/ may touch TTS,
 * SoundPool or Vibration directly.
 */
interface FeedbackDispatcher {
    suspend fun emit(alert: Alert)
    suspend fun interruptCurrent(priority: AlertPriority)
    suspend fun stopAll()
}
