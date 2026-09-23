package com.visionrt.core.orchestration

/**
 * Main assistance session states (ARCHITECTURE §8.1). Transitions are
 * constrained to the graph in §8.2; illegal moves are rejected by
 * [ModeController] so the UI can never observe an impossible state.
 */
enum class OrchestrationState {
    IDLE,
    STARTING,
    OBSTACLE_ASSISTANCE_ACTIVE,
    OBJECT_QUERY_ACTIVE,
    TEXT_READING_ACTIVE,
    DEGRADED,
    ERROR,
    STOPPING;

    /** True when a session is running (any state that may enter STOPPING). */
    fun isActive(): Boolean = when (this) {
        IDLE, STOPPING -> false
        STARTING,
        OBSTACLE_ASSISTANCE_ACTIVE,
        OBJECT_QUERY_ACTIVE,
        TEXT_READING_ACTIVE,
        DEGRADED,
        ERROR,
        -> true
    }

    companion object {
        private val legalTransitions: Map<OrchestrationState, Set<OrchestrationState>> = mapOf(
            IDLE to setOf(STARTING),
            STARTING to setOf(OBSTACLE_ASSISTANCE_ACTIVE, ERROR, STOPPING),
            OBSTACLE_ASSISTANCE_ACTIVE to setOf(
                OBJECT_QUERY_ACTIVE,
                TEXT_READING_ACTIVE,
                DEGRADED,
                ERROR,
                STOPPING,
            ),
            OBJECT_QUERY_ACTIVE to setOf(OBSTACLE_ASSISTANCE_ACTIVE, STOPPING, ERROR),
            TEXT_READING_ACTIVE to setOf(OBSTACLE_ASSISTANCE_ACTIVE, STOPPING, ERROR),
            DEGRADED to setOf(OBSTACLE_ASSISTANCE_ACTIVE, ERROR, STOPPING),
            ERROR to setOf(STOPPING),
            STOPPING to setOf(IDLE),
        )

        fun canTransition(from: OrchestrationState, to: OrchestrationState): Boolean =
            to in legalTransitions.getValue(from)

        fun targets(from: OrchestrationState): Set<OrchestrationState> =
            legalTransitions.getValue(from)
    }
}
