package com.visionrt.feedback

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.domain.EarconType
import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.HapticIntensity
import com.visionrt.core.domain.HapticWaveform
import com.visionrt.core.domain.Proximity
import com.visionrt.core.speech.Speaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PriorityFeedbackDispatcherTest {

    private class RecordingSpeaker : Speaker {
        val spoken = mutableListOf<String>()
        var stopCount = 0
        var lastRate = 1.0f
        var speaking = false

        @Volatile
        override var isReady: Boolean = true

        override fun speak(text: String) {
            if (!isReady || text.isBlank()) return
            speaking = true
            spoken += text
        }

        override fun setSpeechRate(rate: Float) {
            lastRate = rate
        }

        override fun stop() {
            stopCount++
            speaking = false
        }

        override fun shutdown() {
            isReady = false
        }
    }

    private class RecordingHaptics : HapticPlayer {
        val played = mutableListOf<HapticWaveform>()
        var cancelled = false

        override fun play(waveform: HapticWaveform) {
            played += waveform
        }

        override fun cancel() {
            cancelled = true
        }
    }

    private class RecordingEarcons : EarconPlayer {
        val played = mutableListOf<EarconType>()

        override fun play(earcon: EarconType) {
            played += earcon
        }

        override fun stop() = Unit
    }

    private fun alert(
        id: String,
        priority: AlertPriority,
        message: String,
        createdAtMs: Long,
        sectorAndProximity: Pair<HorizontalSector?, Proximity?> = null to null,
    ) = Alert(
        id = id,
        priority = priority,
        message = message,
        createdAtMs = createdAtMs,
        sector = sectorAndProximity.first,
        proximity = sectorAndProximity.second,
    )

    private fun obstacleAlert(
        id: String,
        message: String,
        createdAtMs: Long,
        sector: HorizontalSector,
        proximity: Proximity,
    ) = alert(
        id = id,
        priority = AlertPriority.CRITICAL_OBSTACLE,
        message = message,
        createdAtMs = createdAtMs,
        sectorAndProximity = sector to proximity,
    )

    private fun TestScope.dispatcher(
        speaker: RecordingSpeaker = RecordingSpeaker(),
        haptics: HapticPlayer = RecordingHaptics(),
        earcons: EarconPlayer = RecordingEarcons(),
        settings: FeedbackSettings = FeedbackSettings(),
    ): PriorityFeedbackDispatcher =
        PriorityFeedbackDispatcher(speaker, haptics, earcons, backgroundScope, settings)

    @Test
    fun criticalObstacleEmitsHapticEarconAndSpeech() = runTest {
        val speaker = RecordingSpeaker()
        val haptics = RecordingHaptics()
        val earcons = RecordingEarcons()
        val d = dispatcher(speaker, haptics, earcons)

        d.emit(
            obstacleAlert(
                id = "a1",
                message = "Chair near ahead.",
                createdAtMs = 100,
                sector = HorizontalSector.CENTER,
                proximity = Proximity.NEAR,
            ),
        )
        runCurrent()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, haptics.played.size)
        assertEquals(listOf(EarconType.CENTER), earcons.played)
        assertEquals(listOf("Chair near ahead."), speaker.spoken)
    }

    @Test
    fun speechMutedSkipsSpeechButKeepsHaptics() = runTest {
        val speaker = RecordingSpeaker()
        val haptics = RecordingHaptics()
        val earcons = RecordingEarcons()
        val d = dispatcher(
            speaker,
            haptics,
            earcons,
            settings = FeedbackSettings(speechMuted = true),
        )

        d.emit(
            obstacleAlert(
                id = "a1",
                message = "Person near on left.",
                createdAtMs = 100,
                sector = HorizontalSector.LEFT,
                proximity = Proximity.NEAR,
            ),
        )
        runCurrent()

        assertTrue(speaker.spoken.isEmpty())
        assertEquals(1, haptics.played.size)
        assertEquals(listOf(EarconType.LEFT), earcons.played)
    }

    @Test
    fun hapticsDisabledSkipsVibrationOnly() = runTest {
        val haptics = RecordingHaptics()
        val speaker = RecordingSpeaker()
        val d = dispatcher(
            speaker,
            haptics,
            RecordingEarcons(),
            settings = FeedbackSettings(hapticsEnabled = false),
        )

        d.emit(
            obstacleAlert(
                id = "a1",
                message = "Door near ahead.",
                createdAtMs = 1,
                sector = HorizontalSector.CENTER,
                proximity = Proximity.NEAR,
            ),
        )
        runCurrent()
        advanceTimeBy(300)
        runCurrent()

        assertTrue(haptics.played.isEmpty())
        assertEquals(listOf("Door near ahead."), speaker.spoken)
    }

    @Test
    fun criticalPreemptsPendingLowerPrioritySpeech() = runTest {
        val speaker = RecordingSpeaker()
        val d = dispatcher(speaker)

        // Fill the queue with a long STATUS utterance path + pending DEBUG.
        d.emit(
            alert("status", AlertPriority.STATUS, "Assistance active.", 10),
        )
        d.emit(
            alert("debug", AlertPriority.DEBUG, "Debug frame.", 20),
        )
        d.emit(
            obstacleAlert(
                id = "crit",
                message = "Bike near on right.",
                createdAtMs = 30,
                sector = HorizontalSector.RIGHT,
                proximity = Proximity.NEAR,
            ),
        )
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(
            "Critical must be spoken: ${speaker.spoken}",
            speaker.spoken.contains("Bike near on right."),
        )
        assertTrue(
            "DEBUG must not outlive critical preemption: ${speaker.spoken}",
            !speaker.spoken.contains("Debug frame."),
        )
        assertTrue(speaker.stopCount >= 1)
    }

    @Test
    fun lowerPriorityQueueOrdersCriticalFirst() = runTest {
        val speaker = RecordingSpeaker()
        val d = dispatcher(speaker)

        d.emit(alert("ocr", AlertPriority.OCR_RESULT, "Text ready.", 10))
        d.emit(
            obstacleAlert(
                id = "crit",
                message = "Wall near ahead.",
                createdAtMs = 20,
                sector = HorizontalSector.CENTER,
                proximity = Proximity.NEAR,
            ),
        )
        d.emit(alert("status", AlertPriority.STATUS, "Mode ready.", 5))

        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals("Wall near ahead.", speaker.spoken.first())
    }

    @Test
    fun stopAllClearsQueueAndCancelsChannels() = runTest {
        val speaker = RecordingSpeaker()
        val haptics = RecordingHaptics()
        val d = dispatcher(speaker, haptics)

        d.emit(alert("s1", AlertPriority.STATUS, "One.", 10))
        d.emit(alert("s2", AlertPriority.STATUS, "Two.", 20))
        d.stopAll()
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        assertTrue(speaker.spoken.isEmpty())
        assertTrue(haptics.cancelled)
        assertTrue(speaker.stopCount >= 1)
    }

    @Test
    fun updateSettingsAppliesSpeechRate() = runTest {
        val speaker = RecordingSpeaker()
        val d = dispatcher(speaker)
        d.updateSettings(FeedbackSettings(speechRate = 1.5f))
        assertEquals(1.5f, speaker.lastRate)
    }

    @Test
    fun intensitySettingShapesWaveform() = runTest {
        val haptics = RecordingHaptics()
        val d = dispatcher(
            RecordingSpeaker(),
            haptics,
            RecordingEarcons(),
            settings = FeedbackSettings(hapticIntensity = HapticIntensity.LIGHT),
        )
        d.emit(
            obstacleAlert(
                id = "c",
                message = "Near.",
                createdAtMs = 1,
                sector = HorizontalSector.CENTER,
                proximity = Proximity.NEAR,
            ),
        )
        runCurrent()
        assertEquals(1, haptics.played.size)
        val peak = haptics.played.first().amplitudes.max()
        assertTrue("LIGHT peak should be modest, was $peak", peak in 1..140)
    }

    @Test
    fun statusAlertsDoNotVibrate() = runTest {
        val haptics = RecordingHaptics()
        val earcons = RecordingEarcons()
        val d = dispatcher(RecordingSpeaker(), haptics, earcons)

        d.emit(alert("st", AlertPriority.STATUS, "Assistance stopped.", 10))
        runCurrent()
        advanceTimeBy(200)
        runCurrent()

        assertTrue(haptics.played.isEmpty())
        // No sector → no earcon.
        assertTrue(earcons.played.isEmpty())
    }
}
