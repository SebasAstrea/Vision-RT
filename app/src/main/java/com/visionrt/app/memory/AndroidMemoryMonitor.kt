package com.visionrt.app.memory

import android.app.Application
import android.content.ComponentCallbacks
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Debug
import com.visionrt.core.common.SafeLogger
import com.visionrt.core.memory.MemoryBudgetMonitor
import com.visionrt.core.memory.MemoryPressureBus

/**
 * OR-003: samples process PSS (KB → MB) for [MemoryBudgetMonitor] and
 * forwards Application trim levels to [MemoryPressureBus] (AC5).
 * Bound in DiagnosticsModule (core has no Hilt).
 */
class AndroidMemoryMonitor(
    private val bus: MemoryPressureBus,
) {
    val budget = MemoryBudgetMonitor()

    fun startSession() {
        budget.reset()
        sampleNow()
    }

    fun sampleNow(): Double {
        val mb = Debug.getPss() / KB_PER_MB
        budget.sample(mb)
        return mb
    }

    fun stopSessionAndLog() {
        sampleNow()
        SafeLogger.i(TAG, budget.summaryLine())
        if (!budget.peakWithinBudget()) {
            SafeLogger.w(TAG, "OR-003 peak over ${MemoryBudgetMonitor.PEAK_BUDGET_MB}MB")
        }
        if (!budget.growthWithinBudget()) {
            SafeLogger.w(TAG, "OR-003 growth over ${MemoryBudgetMonitor.MAX_GROWTH_PCT}%")
        }
    }

    fun publishTrim(level: Int) {
        if (level in TRIM_RUNNING_LOW..TRIM_RUNNING_CRITICAL || level >= TRIM_COMPLETE) {
            budget.onLowMemorySignal()
            SafeLogger.w(TAG, "OS low-memory signal level=$level")
        }
        bus.publish(level)
    }

    private companion object {
        const val TAG = "MemoryMonitor"
        const val KB_PER_MB = 1024.0
        const val TRIM_RUNNING_LOW = 10
        const val TRIM_RUNNING_CRITICAL = 15
        const val TRIM_COMPLETE = 80
    }
}

/** Bridges Application.onTrimMemory into the singleton monitor (OR-003.5). */
fun Application.bindMemoryMonitor(monitor: AndroidMemoryMonitor) {
    registerComponentCallbacks(object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) = monitor.publishTrim(level)

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() = monitor.publishTrim(TRIM_MEMORY_COMPLETE)

        override fun onConfigurationChanged(newConfig: Configuration) = Unit
    })
}

// ComponentCallbacks2.TRIM_MEMORY_COMPLETE (API 30+; field is soft-deprecated
// in newer SDK stubs but the runtime contract is unchanged).
private const val TRIM_MEMORY_COMPLETE = 80
