package com.visionrt.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.visionrt.core.domain.EarconType
import com.visionrt.core.domain.HapticWaveform

/**
 * SoundPool-backed earcons (ARCHITECTURE §6.7, UX-005). Three short assets
 * (left/center/right) are preloaded once; [play] is fire-and-forget.
 */
class SoundPoolEarconPlayer(context: Context) : EarconPlayer {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds: Map<EarconType, Int> = mapOf(
        EarconType.LEFT to soundPool.load(context, R.raw.earcon_left, PRIORITY),
        EarconType.CENTER to soundPool.load(context, R.raw.earcon_center, PRIORITY),
        EarconType.RIGHT to soundPool.load(context, R.raw.earcon_right, PRIORITY),
    )

    override fun play(earcon: EarconType) {
        soundIds[earcon]?.let { id ->
            soundPool.play(id, LEFT_VOLUME, RIGHT_VOLUME, PRIORITY, LOOP_OFF, SPEED)
        }
    }

    override fun stop() = Unit

    fun release() {
        soundPool.release()
    }

    private companion object {
        const val MAX_STREAMS = 2
        const val PRIORITY = 1
        const val LOOP_OFF = 0
        const val LEFT_VOLUME = 0.9f
        const val RIGHT_VOLUME = 0.9f
        const val SPEED = 1.0f
    }
}

/**
 * VibrationEffect player (FR-011). Amplitude waveform is preferred so
 * intensity settings map to real motor strength; falls back to timing-only
 * when the device rejects amplitudes.
 */
class AndroidHapticPlayer(context: Context) : HapticPlayer {

    private val vibrator: Vibrator = resolveVibrator(context)

    override fun play(waveform: HapticWaveform) {
        if (!vibrator.hasVibrator()) return
        val timings = waveform.timings
        val amplitudes = waveform.amplitudes
        try {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } catch (_: RuntimeException) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }

    override fun cancel() {
        vibrator.cancel()
    }

    private companion object {
        fun resolveVibrator(context: Context): Vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
                    ?: context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
    }
}
