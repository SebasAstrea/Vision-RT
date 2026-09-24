package com.visionrt.core.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.visionrt.core.common.SafeLogger

/**
 * [Speaker] backed by the device TextToSpeech engine (ARCHITECTURE.md §6.6).
 * Initialization is asynchronous; [speak] is a no-op until the engine is ready
 * so callers never block. Engine failures are swallowed (§12.5: TTS failure
 * must not crash or block the app).
 */
class SystemSpeaker(context: Context) : Speaker {

    private var engine: TextToSpeech? = null

    @Volatile
    override var isReady: Boolean = false
        private set

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            val tts = engine ?: return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = Unit

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) = Unit
                })
                isReady = true
            } else {
                SafeLogger.w(TAG, "TTS init failed status=$status")
                isReady = false
            }
        }
    }

    override fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        engine?.setSpeechRate(clamped)
    }

    override fun stop() {
        engine?.stop()
    }

    override fun shutdown() {
        isReady = false
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    private companion object {
        const val TAG = "SystemSpeaker"
        const val UTTERANCE_ID = "visionrt-ui"
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
    }
}
