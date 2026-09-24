package com.visionrt.feedback

import com.visionrt.core.domain.EarconType
import com.visionrt.core.domain.HapticWaveform

/**
 * Short non-speech cues (ARCHITECTURE §12.1, FR-010). Implementations wrap
 * SoundPool; tests use recording fakes. Must never block the caller.
 */
interface EarconPlayer {
    fun play(earcon: EarconType)

    fun stop()
}

/**
 * Haptic channel (FR-011). Implementations wrap VibrationEffect; tests use
 * recording fakes. Playback must start within 300 ms of the alert event.
 */
interface HapticPlayer {
    fun play(waveform: HapticWaveform)

    fun cancel()
}

/** No-op players for mute/pause paths and unit tests. */
object NoopEarconPlayer : EarconPlayer {
    override fun play(earcon: EarconType) = Unit
    override fun stop() = Unit
}

object NoopHapticPlayer : HapticPlayer {
    override fun play(waveform: HapticWaveform) = Unit
    override fun cancel() = Unit
}
