package com.visionrt.feature.voice

import com.visionrt.core.speech.Speaker
import com.visionrt.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Reads screen content with the system TTS for users without TalkBack
 * (FR-010 / ARCHITECTURE.md §6.6). When an accessibility service (e.g.
 * TalkBack) is active it stays silent so the screen reader remains the single
 * voice (NFR-COMP-002, UX-002). Respects the persisted speech mute used by
 * the home silence control.
 */
class ScreenVoice @Inject constructor(
    private val speaker: Speaker,
    private val settings: SettingsRepository,
    private val screenReader: ScreenReaderGate,
) {

    /** Narrates [text] unless muted, TalkBack is on, or the engine is not ready. */
    suspend fun narrate(text: String) {
        if (text.isNotBlank() && !settings.speechMuted.first() && !screenReader.isScreenReaderActive()) {
            speaker.speak(text)
        }
    }

    /**
     * Speaks [text] regardless of TalkBack (practice alerts that exercise the
     * app TTS path). Still respects the explicit speech mute.
     */
    suspend fun speakNow(text: String) {
        if (text.isNotBlank() && !settings.speechMuted.first()) {
            speaker.speak(text)
        }
    }

    fun stop() = speaker.stop()
}
