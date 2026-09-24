package com.visionrt.feature.voice

import com.visionrt.core.domain.HapticIntensity
import com.visionrt.core.domain.Verbosity
import com.visionrt.core.speech.Speaker
import com.visionrt.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenVoiceTest {

    private class RecordingSpeaker : Speaker {
        val spoken = mutableListOf<String>()
        var stopped = false

        @Volatile
        override var isReady: Boolean = true

        override fun speak(text: String) {
            if (!isReady || text.isBlank()) return
            spoken += text
        }

        override fun stop() {
            stopped = true
        }

        override fun shutdown() {
            isReady = false
        }
    }

    private class FakeSettings : SettingsRepository {
        private val mutedFlow = MutableStateFlow(false)

        override val onboardingAcknowledged: Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setOnboardingAcknowledged(acknowledged: Boolean) = Unit
        override val trainingCompleted: Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setTrainingCompleted(completed: Boolean) = Unit
        override val verbosity: Flow<Verbosity> = MutableStateFlow(Verbosity.NORMAL)
        override suspend fun setVerbosity(verbosity: Verbosity) = Unit
        override val speechMuted: Flow<Boolean> = mutedFlow

        override suspend fun setSpeechMuted(muted: Boolean) {
            mutedFlow.value = muted
        }

        override val speechRate: Flow<Float> = MutableStateFlow(1.0f)
        override suspend fun setSpeechRate(rate: Float) = Unit
        override val hapticsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setHapticsEnabled(enabled: Boolean) = Unit
        override val hapticIntensity: Flow<HapticIntensity> =
            MutableStateFlow(HapticIntensity.MEDIUM)

        override suspend fun setHapticIntensity(intensity: HapticIntensity) = Unit
        override val earconsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setEarconsEnabled(enabled: Boolean) = Unit
    }

    private fun voice(
        speaker: RecordingSpeaker,
        settings: SettingsRepository = FakeSettings(),
        screenReaderActive: Boolean = false,
    ) = ScreenVoice(speaker, settings, ScreenReaderGate { screenReaderActive })

    @Test
    fun narratesWhenNotMutedAndNoScreenReader() = runBlocking {
        val speaker = RecordingSpeaker()
        voice(speaker).narrate("Home screen")
        assertEquals(listOf("Home screen"), speaker.spoken)
    }

    @Test
    fun doesNotNarrateWhenMuted() = runBlocking {
        val speaker = RecordingSpeaker()
        val settings = FakeSettings()
        settings.setSpeechMuted(true)
        voice(speaker, settings).narrate("Home screen")
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun doesNotNarrateWhenScreenReaderActive() = runBlocking {
        val speaker = RecordingSpeaker()
        voice(speaker, screenReaderActive = true).narrate("Home screen")
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun doesNotNarrateBlankText() = runBlocking {
        val speaker = RecordingSpeaker()
        voice(speaker).narrate("   ")
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun speakNowIgnoresScreenReaderButRespectsMute() = runBlocking {
        val speaker = RecordingSpeaker()
        voice(speaker, screenReaderActive = true).speakNow("Possible obstacle ahead.")
        assertEquals(listOf("Possible obstacle ahead."), speaker.spoken)

        val settings = FakeSettings()
        settings.setSpeechMuted(true)
        val mutedSpeaker = RecordingSpeaker()
        voice(mutedSpeaker, settings).speakNow("Possible obstacle ahead.")
        assertTrue(mutedSpeaker.spoken.isEmpty())
    }

    @Test
    fun stopDelegatesToSpeaker() {
        val speaker = RecordingSpeaker()
        voice(speaker).stop()
        assertTrue(speaker.stopped)
    }
}
