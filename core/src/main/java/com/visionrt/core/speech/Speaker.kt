package com.visionrt.core.speech

/**
 * Spoken-output channel for UI narration (ARCHITECTURE.md §6.6, §12.1).
 * Implementations wrap the system TextToSpeech engine; alert feedback continues
 * to flow through FeedbackDispatcher (feedback/) in M2+. Callers outside
 * core must not construct TextToSpeech directly.
 */
interface Speaker {
    /** True once the underlying engine finished OnInit successfully. */
    val isReady: Boolean

    /** Queues [text] for speech. No-op when not ready or [text] is blank. */
    fun speak(text: String)

    /**
     * App-level speech rate override in 0.5x–2.0x (FR-010.5). Engines that
     * cannot change rate at runtime may ignore the value.
     */
    fun setSpeechRate(rate: Float) = Unit

    /** Stops any in-flight utterance and clears the queue. */
    fun stop()

    /** Releases the engine; the instance must not be used afterwards. */
    fun shutdown()
}
