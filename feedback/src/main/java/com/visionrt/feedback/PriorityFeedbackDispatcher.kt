package com.visionrt.feedback

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.domain.HapticIntensity
import com.visionrt.core.domain.HapticPatterns
import com.visionrt.core.speech.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * User-facing feedback knobs the dispatcher must respect on every emit
 * (ARCHITECTURE §12.3.5, FR-010/FR-011/FR-012). Flows are hot so a settings
 * change applies to the next alert without restarting the dispatcher.
 */
data class FeedbackSettings(
    val speechMuted: Boolean = false,
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    val hapticsEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val earconsEnabled: Boolean = true,
) {
    private companion object {
        const val DEFAULT_SPEECH_RATE = 1.0f
    }
}

/**
 * Production [FeedbackDispatcher] (M4): priority queue + preemption +
 * multi-channel emit.
 *
 * Ordering rules (ARCHITECTURE §12.2–12.3, OR-009):
 * 1. [AlertPriority] is the primary key (CRITICAL first).
 * 2. [Alert.createdAtMs] breaks ties (older first).
 * 3. Critical obstacles flush lower-priority pending speech and stop any
 *    in-flight utterance before speaking (FR-006.3).
 * 4. Haptics and earcons fire immediately on emit — they never wait for the
 *    speech queue (FR-010.3, FR-011.4, Bluetooth-latency mitigation).
 * 5. Speech respects mute and rate; haptics/earcons respect their own flags.
 *
 * The dispatcher owns a single worker so utterances never overlap.
 */
class PriorityFeedbackDispatcher(
    private val speaker: Speaker,
    private val haptics: HapticPlayer,
    private val earcons: EarconPlayer,
    private val scope: CoroutineScope,
    initialSettings: FeedbackSettings = FeedbackSettings(),
) : FeedbackDispatcher {

    private val settingsMutex = Mutex()
    private var settings: FeedbackSettings = initialSettings

    private val queueMutex = Mutex()
    private val pending = ArrayDeque<Alert>()
    private var workerActive = false

    private val control = Channel<Control>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (cmd in control) {
                when (cmd) {
                    is Control.Pump -> pump()
                    Control.Shutdown -> break
                }
            }
        }
    }

    /** Applies the latest user preferences (call from a settings collector). */
    suspend fun updateSettings(next: FeedbackSettings) {
        settingsMutex.withLock { settings = next }
        if (next.speechMuted) {
            speaker.stop()
        }
        speaker.setSpeechRate(next.speechRate)
    }

    override suspend fun emit(alert: Alert) {
        if (alert.message.isBlank()) return
        val current = currentSettings()

        // Non-speech channels first so latency budgets hold even if speech
        // is muted or the queue is busy.
        if (current.hapticsEnabled && alert.priority == AlertPriority.CRITICAL_OBSTACLE) {
            haptics.play(
                HapticPatterns.waveform(
                    sector = alert.sector,
                    proximity = alert.proximity,
                    intensity = current.hapticIntensity,
                ),
            )
        }
        if (current.earconsEnabled && alert.sector != null) {
            earcons.play(HapticPatterns.earconFor(alert.sector))
        }

        if (current.speechMuted) return

        queueMutex.withLock {
            if (alert.priority == AlertPriority.CRITICAL_OBSTACLE) {
                // Preempt: drop weaker pending speech (FR-006.3).
                pending.removeAll { it.priority.ordinal > AlertPriority.CRITICAL_OBSTACLE.ordinal }
                speaker.stop()
            }
            insertByPriority(alert)
            if (!workerActive) {
                workerActive = true
                control.trySend(Control.Pump)
            }
        }
    }

    override suspend fun interruptCurrent(priority: AlertPriority) {
        if (priority == AlertPriority.CRITICAL_OBSTACLE) {
            queueMutex.withLock {
                pending.removeAll { it.priority.ordinal >= priority.ordinal }
            }
            speaker.stop()
        }
    }

    override suspend fun stopAll() {
        queueMutex.withLock {
            pending.clear()
            workerActive = false
        }
        speaker.stop()
        haptics.cancel()
        earcons.stop()
    }

    private suspend fun currentSettings(): FeedbackSettings = settingsMutex.withLock { settings }

    private fun insertByPriority(alert: Alert) {
        val index = pending.indexOfFirst { existing ->
            existing.priority.ordinal > alert.priority.ordinal ||
                (
                    existing.priority.ordinal == alert.priority.ordinal &&
                        existing.createdAtMs > alert.createdAtMs
                    )
        }
        if (index < 0) pending.addLast(alert) else pending.add(index, alert)
    }

    private suspend fun pump() {
        while (true) {
            val next = queueMutex.withLock {
                val head = pending.removeFirstOrNull()
                if (head == null) {
                    workerActive = false
                    null
                } else {
                    head
                }
            } ?: break

            val current = currentSettings()
            if (!current.speechMuted) {
                speaker.setSpeechRate(current.speechRate)
                speaker.speak(next.message)
                // Approximate utterance length so the next phrase does not
                // overlap; TTS QUEUE_FLUSH still guards real overlaps.
                awaitApproximateUtterance(next.message)
            }
        }
        // Re-check in case emit raced with the empty-queue transition.
        val hasMore = queueMutex.withLock { pending.isNotEmpty() }
        if (hasMore) control.trySend(Control.Pump)
    }

    private suspend fun awaitApproximateUtterance(message: String) {
        val words = message.trim().split(WHITESPACE).size
        val rate = currentSettings().speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        val rateSteps = rate.toLong().coerceAtLeast(1L)
        val ms = ((words * MS_PER_WORD) / rateSteps).coerceAtLeast(MIN_UTTERANCE_MS)
        delay(ms)
    }

    private sealed interface Control {
        data object Pump : Control
        data object Shutdown : Control
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        const val MS_PER_WORD = 140L
        const val MIN_UTTERANCE_MS = 120L
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
    }
}
