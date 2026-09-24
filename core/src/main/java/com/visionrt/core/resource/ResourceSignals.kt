package com.visionrt.core.resource

private const val LEVEL_NORMAL = 0
private const val LEVEL_REDUCED = 1
private const val LEVEL_MINIMAL = 2
private const val LEVEL_CRITICAL = 3

/**
 * Degradation ladder (ARCHITECTURE §9.4 / M6 deliverable 6).
 *
 * Level increases under thermal, battery, memory or latency pressure and
 * decreases when signals improve (OR-006.4 recovery with accessible notice).
 */
enum class DegradationLevel(val priority: Int) {
    /** Level 0: normal low-end mode (detector 320, adaptive FPS). */
    NORMAL(LEVEL_NORMAL),

    /** Level 1: reduced — FPS −50%, optional features off (OR-006.1). */
    REDUCED(LEVEL_REDUCED),

    /** Level 2: minimal — very low FPS, user notified (OR-006.2-ish). */
    MINIMAL(LEVEL_MINIMAL),

    /** Level 3: critical — continuous detection stopped (OR-006.2). */
    CRITICAL(LEVEL_CRITICAL);

    fun isAtLeast(other: DegradationLevel): Boolean = priority >= other.priority
}

/** Snapshot of external resource signals (ARCHITECTURE §9.2 inputs). */
data class ResourceSignals(
    /** Android PowerManager thermal status ordinal 0..6 (NONE..EMERGENCY). */
    val thermalStatus: Int = THERMAL_NONE,
    val batterySaverActive: Boolean = false,
    /** Battery percent 0..100; [BATTERY_UNKNOWN] when unavailable. */
    val batteryPercent: Int = BATTERY_UNKNOWN,
    /** Peak process memory in MB (0 when no sample). */
    val memoryPeakMb: Double = MEMORY_NONE_MB,
    /** Recent inference latency P95 in ms; 0 when unknown. */
    val latencyP95Ms: Double = LATENCY_NONE_MS,
    /** True while camera frames are flowing. */
    val cameraAvailable: Boolean = true,
) {
    companion object {
        const val THERMAL_NONE = 0
        const val THERMAL_LIGHT = 1
        const val THERMAL_MODERATE = 2
        const val THERMAL_SEVERE = 3
        const val THERMAL_CRITICAL = 4
        const val THERMAL_EMERGENCY = 5
        const val THERMAL_SHUTDOWN = 6
        const val BATTERY_UNKNOWN = -1
        const val MEMORY_NONE_MB = 0.0
        const val LATENCY_NONE_MS = 0.0

        /** ARCHITECTURE §16.3 */
        const val MEMORY_WARN_MB = 650.0
        const val MEMORY_CRITICAL_MB = 750.0

        /** OR-004.6 / NFR-PE-002: sustained P95 over detector budget. */
        const val LATENCY_BUDGET_MS = 450.0
        const val BATTERY_LOW_PERCENT = 15
        const val BATTERY_CRITICAL_PERCENT = 5

        val DEFAULT = ResourceSignals()
    }
}

/** Cause of a level change (diagnostics + accessible announcement). */
enum class ResourceCause {
    NONE,
    THERMAL,
    BATTERY_SAVER,
    BATTERY_LOW,
    MEMORY,
    LATENCY,
    CAMERA_LOST,
    RECOVERED,
}
