package com.visionrt.core.memory

import java.util.Locale

/**
 * Tracks process memory against OR-003 budgets and ARCHITECTURE §16.3
 * pressure levels. Pure Kotlin; the app feeds samples (PSS or equivalent, MB).
 *
 * Budgets:
 * - Peak ≤ [PEAK_BUDGET_MB] (OR-003.1 / NFR-PE-003)
 * - Steady-state growth ≤ [MAX_GROWTH_PCT] of baseline (OR-003.3)
 * - WARN ≥ 650 MB, CRITICAL ≥ 750 MB (ARCHITECTURE §16.3)
 */
class MemoryBudgetMonitor(
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    private var baselineMb: Double? = null
    private var lastSampleMb: Double = 0.0
    private var lastSampleAtMs: Long = 0L

    var peakMb: Double = 0.0
        private set

    var sampleCount: Int = 0
        private set

    var lowMemoryEventCount: Int = 0
        private set

    /** Clears session state (called when assistance starts). */
    fun reset() {
        baselineMb = null
        lastSampleMb = 0.0
        lastSampleAtMs = 0L
        peakMb = 0.0
        sampleCount = 0
        lowMemoryEventCount = 0
    }

    /**
     * Records one sample. First sample after [reset] becomes the baseline
     * (OR-003.3). [usedMb] must be non-negative.
     */
    fun sample(usedMb: Double, timestampMs: Long = clock()) {
        require(usedMb >= 0.0) { "usedMb must be non-negative" }
        if (baselineMb == null) baselineMb = usedMb
        lastSampleMb = usedMb
        lastSampleAtMs = timestampMs
        sampleCount++
        if (usedMb > peakMb) peakMb = usedMb
    }

    /** OS low-memory callback observed (OR-003.5 / AC5). */
    fun onLowMemorySignal() {
        lowMemoryEventCount++
    }

    fun baseline(): Double? = baselineMb

    /** Growth of peak vs baseline in percent; null until baseline exists. */
    fun peakGrowthPct(): Double? {
        val base = baselineMb
        if (base == null || base <= 0.0) return null
        return ((peakMb - base) / base) * PERCENT_SCALE
    }

    fun peakWithinBudget(): Boolean = peakMb <= PEAK_BUDGET_MB

    fun growthWithinBudget(): Boolean {
        val g = peakGrowthPct() ?: return true
        return g <= MAX_GROWTH_PCT
    }

    fun pressureLevel(): MemoryPressureLevel = when {
        peakMb >= CRITICAL_MB -> MemoryPressureLevel.CRITICAL
        peakMb >= WARN_MB -> MemoryPressureLevel.WARN
        else -> MemoryPressureLevel.OK
    }

    fun summaryLine(): String {
        val growth = peakGrowthPct()?.let { fmt(it) } ?: "n/a"
        return "peakMb=${fmt(peakMb)} " +
            "baselineMb=${baselineMb?.let { fmt(it) } ?: "n/a"} " +
            "growthPct=$growth " +
            "samples=$sampleCount " +
            "peakOk=${peakWithinBudget()} " +
            "growthOk=${growthWithinBudget()} " +
            "level=${pressureLevel().name} " +
            "trimEvents=$lowMemoryEventCount"
    }

    private fun fmt(value: Double): String =
        String.format(Locale.US, "%.1f", value)

    companion object {
        const val PEAK_BUDGET_MB = 800.0
        const val MAX_GROWTH_PCT = 10.0
        const val WARN_MB = 650.0
        const val CRITICAL_MB = 750.0
        private const val PERCENT_SCALE = 100.0
    }
}

enum class MemoryPressureLevel { OK, WARN, CRITICAL }

/**
 * Fan-out for Application.onTrimMemory → orchestration (OR-003.5).
 * Listeners are plain interfaces so core stays free of Android types.
 */
class MemoryPressureBus {
    fun interface Listener {
        /** @param level ComponentCallbacks2 trim level (application constants). */
        fun onTrimMemory(level: Int)
    }

    private val listeners = LinkedHashSet<Listener>()

    @Synchronized
    fun add(listener: Listener) {
        listeners.add(listener)
    }

    @Synchronized
    fun remove(listener: Listener) {
        listeners.remove(listener)
    }

    @Synchronized
    fun publish(level: Int) {
        listeners.forEach { it.onTrimMemory(level) }
    }

    @Synchronized
    fun listenerCount(): Int = listeners.size
}
