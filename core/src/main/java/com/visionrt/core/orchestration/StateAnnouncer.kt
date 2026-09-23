package com.visionrt.core.orchestration

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority

/**
 * Speaks accepted mode transitions as STATUS alerts so no UI-visible state
 * change is silent (M2 exit criteria, FR-015).
 */
internal class StateAnnouncer(
    private val feedback: FeedbackPort,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun announce(from: OrchestrationState, to: OrchestrationState) {
        val message = announcement(from, to) ?: return
        feedback.emit(
            Alert(
                id = "state-$from-$to-${clock()}",
                priority = AlertPriority.STATUS,
                message = message,
                createdAtMs = clock(),
            ),
        )
    }

    private fun announcement(from: OrchestrationState, to: OrchestrationState): String? =
        when (to) {
            OrchestrationState.STARTING -> "Starting assistance."
            OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE ->
                if (from == OrchestrationState.STARTING) {
                    "Obstacle assistance ready."
                } else {
                    "Obstacle assistance active."
                }
            OrchestrationState.OBJECT_QUERY_ACTIVE -> "Object query ready."
            OrchestrationState.TEXT_READING_ACTIVE -> "Text reading ready."
            OrchestrationState.DEGRADED -> "Assistance is limited."
            OrchestrationState.ERROR -> "Assistance stopped due to an error."
            OrchestrationState.STOPPING -> "Stopping assistance."
            OrchestrationState.IDLE ->
                if (from == OrchestrationState.STOPPING) "Assistance stopped." else null
        }
}
