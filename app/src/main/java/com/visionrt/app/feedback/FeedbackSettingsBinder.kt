package com.visionrt.app.feedback

import com.visionrt.data.settings.SettingsRepository
import com.visionrt.feedback.FeedbackSettings
import com.visionrt.feedback.PriorityFeedbackDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * M4: pushes persisted feedback preferences into the dispatcher so mute,
 * speech rate, haptics and earcons apply immediately (FR-012).
 */
class FeedbackSettingsBinder(
    private val settings: SettingsRepository,
    private val dispatcher: PriorityFeedbackDispatcher,
) {
    fun bind(scope: CoroutineScope) {
        scope.launch {
            combine(
                settings.speechMuted,
                settings.speechRate,
                settings.hapticsEnabled,
                settings.hapticIntensity,
                settings.earconsEnabled,
            ) { muted, rate, haptics, intensity, earcons ->
                FeedbackSettings(
                    speechMuted = muted,
                    speechRate = rate,
                    hapticsEnabled = haptics,
                    hapticIntensity = intensity,
                    earconsEnabled = earcons,
                )
            }.collect { dispatcher.updateSettings(it) }
        }
    }
}
