package com.visionrt.core.resource

/**
 * Pure resource governor (ARCHITECTURE §9.1–9.4, OR-005/006/007/010).
 *
 * Maps [ResourceSignals] → [DegradationLevel] + frame-interval multiplier.
 * No Android types; the app module samples PowerManager/BatteryManager and
 * feeds signals on each tick. Level only escalates while pressure persists
 * and steps down when the worst signal clears (OR-006.4).
 */
class ResourceGovernor(
    private val baseFrameIntervalMs: Long,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    var level: DegradationLevel = DegradationLevel.NORMAL
        private set

    var lastCause: ResourceCause = ResourceCause.NONE
        private set

    var lastChangeAtMs: Long = 0L
        private set

    private var batterySaverActive: Boolean = false
    private var consecutiveTicksAtLevel: Int = 0

    /**
     * Evaluates [signals] and returns the (possibly new) level.
     * Transitions require [HOLD_TICKS] consecutive evaluations at the desired
     * level so a single noisy sample cannot flap the ladder.
     */
    fun evaluate(signals: ResourceSignals): DegradationLevel {
        batterySaverActive = signals.batterySaverActive
        val desired = desiredLevel(signals)
        val cause = causeFor(signals, desired)
        return if (desired != level) {
            consecutiveTicksAtLevel++
            if (consecutiveTicksAtLevel >= HOLD_TICKS) {
                if (desired.priority > level.priority) {
                    escalate(desired, cause)
                } else {
                    recover(desired)
                }
            } else {
                if (cause != ResourceCause.NONE) lastCause = cause
                level
            }
        } else {
            consecutiveTicksAtLevel = 0
            level
        }
    }

    /**
     * Frame delay for the current ladder level (OR-005 / OR-006.1 / OR-007.2).
     * Battery saver enforces FPS ≤ 3 → interval ≥ 333 ms regardless of level.
     */
    fun frameIntervalMs(): Long {
        val multiplier = when (level) {
            DegradationLevel.NORMAL -> 1L
            DegradationLevel.REDUCED -> 2L
            DegradationLevel.MINIMAL -> MINIMAL_MULTIPLIER
            DegradationLevel.CRITICAL -> CRITICAL_MULTIPLIER
        }
        var interval = baseFrameIntervalMs * multiplier
        if (interval > MAX_INTERVAL_MS) interval = MAX_INTERVAL_MS
        if (batterySaverActive && interval < BATTERY_SAVER_MIN_INTERVAL_MS) {
            interval = BATTERY_SAVER_MIN_INTERVAL_MS
        }
        return interval
    }

    /** True when continuous detection must stop (ARCHITECTURE §9.4 L3). */
    fun continuousDetectionStopped(): Boolean = level == DegradationLevel.CRITICAL

    /** True when optional models (OCR etc.) must stay unloaded. */
    fun optionalModelsDisabled(): Boolean = level.isAtLeast(DegradationLevel.REDUCED)

    private fun desiredLevel(s: ResourceSignals): DegradationLevel = when {
        // Severe+ thermal: stop continuous detection (OR-006.2).
        s.thermalStatus >= ResourceSignals.THERMAL_SEVERE -> DegradationLevel.CRITICAL
        // Camera lost: cannot detect (OR-010.2) — critical for continuous mode.
        !s.cameraAvailable -> DegradationLevel.CRITICAL
        // Memory critical: unload optionals + minimal (OR-010.4 / §16.3).
        s.memoryPeakMb >= ResourceSignals.MEMORY_CRITICAL_MB -> DegradationLevel.MINIMAL
        // Battery critical while active: very low frequency (OR-007).
        s.batteryPercent in 0 until ResourceSignals.BATTERY_CRITICAL_PERCENT ->
            DegradationLevel.MINIMAL
        // Battery saver: FPS ≤ 3 fps → interval ≥ 333 ms (OR-007.2).
        s.batterySaverActive -> DegradationLevel.REDUCED
        // Thermal moderate: FPS −50% (OR-006.1).
        s.thermalStatus >= ResourceSignals.THERMAL_MODERATE -> DegradationLevel.REDUCED
        // Memory warn: reduce optional load (OR-010.4).
        s.memoryPeakMb >= ResourceSignals.MEMORY_WARN_MB -> DegradationLevel.REDUCED
        // Sustained latency over budget (OR-004.6).
        s.latencyP95Ms > ResourceSignals.LATENCY_BUDGET_MS -> DegradationLevel.REDUCED
        // Low battery: notify path, mild reduction (OR-007.3).
        s.batteryPercent in 0 until ResourceSignals.BATTERY_LOW_PERCENT -> DegradationLevel.REDUCED
        else -> DegradationLevel.NORMAL
    }

    private fun causeFor(s: ResourceSignals, desired: DegradationLevel): ResourceCause = when {
        s.thermalStatus >= ResourceSignals.THERMAL_MODERATE -> ResourceCause.THERMAL
        !s.cameraAvailable -> ResourceCause.CAMERA_LOST
        s.memoryPeakMb >= ResourceSignals.MEMORY_WARN_MB -> ResourceCause.MEMORY
        s.batterySaverActive -> ResourceCause.BATTERY_SAVER
        s.batteryPercent in 0 until ResourceSignals.BATTERY_LOW_PERCENT -> ResourceCause.BATTERY_LOW
        s.latencyP95Ms > ResourceSignals.LATENCY_BUDGET_MS -> ResourceCause.LATENCY
        desired == DegradationLevel.NORMAL && level != DegradationLevel.NORMAL ->
            ResourceCause.RECOVERED
        else -> ResourceCause.NONE
    }

    private fun escalate(to: DegradationLevel, cause: ResourceCause): DegradationLevel {
        level = to
        lastCause = cause
        lastChangeAtMs = clock()
        consecutiveTicksAtLevel = 0
        return level
    }

    private fun recover(to: DegradationLevel): DegradationLevel {
        level = to
        lastCause = if (to == DegradationLevel.NORMAL) ResourceCause.RECOVERED else lastCause
        lastChangeAtMs = clock()
        consecutiveTicksAtLevel = 0
        return level
    }

    fun reset() {
        level = DegradationLevel.NORMAL
        lastCause = ResourceCause.NONE
        lastChangeAtMs = 0L
        batterySaverActive = false
        consecutiveTicksAtLevel = 0
    }

    companion object {
        /** Ticks of agreement before the ladder moves (hysteresis). */
        const val HOLD_TICKS = 2

        /** Battery saver: FPS ≤ 3 → interval ≥ 333 ms (OR-007.2). */
        const val BATTERY_SAVER_MIN_INTERVAL_MS = 333L

        private const val MINIMAL_MULTIPLIER = 4L
        private const val CRITICAL_MULTIPLIER = 8L
        private const val MAX_INTERVAL_MS = 2_000L
    }
}
