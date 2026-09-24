package com.visionrt.data.settings

/**
 * Feedback preference keys owned by the data module (FR-010.5, FR-011.5–6,
 * FR-012.1/3/4). Values are read by the app layer and pushed into
 * [com.visionrt.feedback.FeedbackSettings].
 */
object FeedbackPreferenceKeys {
    const val SPEECH_RATE = "speech_rate"
    const val HAPTICS_ENABLED = "haptics_enabled"
    const val HAPTIC_INTENSITY = "haptic_intensity"
    const val EARCONS_ENABLED = "earcons_enabled"
}
