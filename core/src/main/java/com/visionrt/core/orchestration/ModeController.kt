package com.visionrt.core.orchestration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the mode state machine (ARCHITECTURE §8, §9.1).
 *
 * Every accepted transition is announced through [StateAnnouncer] as a STATUS
 * alert so no UI-visible state change is silent (M2 exit criteria, FR-015).
 * Illegal transitions return false and leave the state untouched.
 */
class ModeController(
    private val feedback: FeedbackPort,
    private val clock: () -> Long = { System.currentTimeMillis() },
    lang: AlertLang = AlertLang.current(),
) {
    private val announcer = StateAnnouncer(feedback, clock, lang)
    private val _state = MutableStateFlow(OrchestrationState.IDLE)
    val state: StateFlow<OrchestrationState> = _state.asStateFlow()

    val current: OrchestrationState
        get() = _state.value

    suspend fun transition(to: OrchestrationState): Boolean {
        val from = _state.value
        if (!OrchestrationState.canTransition(from, to)) return false
        _state.value = to
        announcer.announce(from, to)
        return true
    }

    suspend fun start(): Boolean = transition(OrchestrationState.STARTING)

    suspend fun onReady(): Boolean = transition(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE)

    suspend fun enterObjectQuery(): Boolean =
        transition(OrchestrationState.OBJECT_QUERY_ACTIVE)

    suspend fun enterTextReading(): Boolean =
        transition(OrchestrationState.TEXT_READING_ACTIVE)

    suspend fun returnToObstacleAssistance(): Boolean =
        transition(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE)

    /** True when a return to continuous obstacle assistance is legal. */
    fun canReturnToObstacle(): Boolean =
        OrchestrationState.canTransition(current, OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE)

    suspend fun degrade(): Boolean = transition(OrchestrationState.DEGRADED)

    /** Convenience: stop from any active state, then return to idle. */
    suspend fun stopSession(): Boolean {
        if (current != OrchestrationState.STOPPING) {
            if (!transition(OrchestrationState.STOPPING)) return false
            feedback.stopAll()
        }
        return transition(OrchestrationState.IDLE)
    }
}
