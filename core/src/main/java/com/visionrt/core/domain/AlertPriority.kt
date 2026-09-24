package com.visionrt.core.domain

/**
 * Voice+earcon dispatch priority (ARCHITECTURE §9.1).
 *
 * Declaration order is **not** semantic (CRITICAL first is convenient for
 * `when` branches), so use [rank] for severity comparisons. Higher rank =
 * more severe.
 */
enum class AlertPriority {
    CRITICAL_OBSTACLE,
    USER_REQUESTED,
    OCR_RESULT,
    STATUS,
    DEBUG,
    ;

    /**
     * Severity rank for comparisons. Higher = more severe.
     * Never rely on [ordinal] for this — the declaration order exists only
     * for readability.
     */
    val rank: Int
        get() = when (this) {
            CRITICAL_OBSTACLE -> 4
            USER_REQUESTED -> 3
            OCR_RESULT -> 2
            STATUS -> 1
            DEBUG -> 0
        }
}
