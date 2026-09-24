package com.visionrt.core.orchestration.fake

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.orchestration.FeedbackPort
import java.util.concurrent.atomic.AtomicReference

/**
 * Recording TTS/haptics double (M2 deliverable). Captures every emit,
 * interrupt and stopAll so tests can assert priority and interruption without
 * touching real audio or vibration APIs.
 *
 * [currentPriority] is a **method** (matching [FeedbackPort]). Tests read it
 * as `feedback.currentPriority()`.
 */
class FakeFeedbackPort : FeedbackPort {

    private val _emitted = mutableListOf<Alert>()
    val emitted: List<Alert> get() = _emitted

    private val _interrupts = mutableListOf<AlertPriority>()
    val interrupts: List<AlertPriority> get() = _interrupts

    private val inFlight = AtomicReference<AlertPriority?>(null)

    var stopAllCount: Int = 0
        private set

    override suspend fun emit(alert: Alert) {
        _emitted += alert
        inFlight.set(alert.priority)
    }

    override suspend fun interruptCurrent(priority: AlertPriority) {
        _interrupts += priority
        val current = inFlight.get()
        if (current != null && current.rank <= priority.rank) {
            inFlight.set(null)
        }
    }

    override suspend fun stopAll() {
        stopAllCount++
        inFlight.set(null)
    }

    override fun currentPriority(): AlertPriority? = inFlight.get()

    override fun isActive(): Boolean = inFlight.get() != null

    fun messages(): List<String> = _emitted.map { it.message }

    fun clear() {
        _emitted.clear()
        _interrupts.clear()
        stopAllCount = 0
        inFlight.set(null)
    }
}
