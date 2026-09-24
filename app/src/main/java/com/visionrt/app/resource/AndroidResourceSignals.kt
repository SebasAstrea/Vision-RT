package com.visionrt.app.resource

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.visionrt.core.resource.ResourceSignals
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Samples Android thermal/battery inputs for [ResourceGovernor] (M6,
 * OR-006 / OR-007). Memory peak and latency are fed by the coordinator.
 */
@Singleton
class AndroidResourceSignals @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun sample(
        memoryPeakMb: Double = 0.0,
        latencyP95Ms: Double = 0.0,
        cameraAvailable: Boolean = true,
    ): ResourceSignals {
        val thermal = powerManager.currentThermalStatus
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            (level * PERCENT_SCALE) / scale
        } else {
            ResourceSignals.BATTERY_UNKNOWN
        }
        return ResourceSignals(
            thermalStatus = thermal,
            batterySaverActive = powerManager.isPowerSaveMode,
            batteryPercent = percent,
            memoryPeakMb = memoryPeakMb,
            latencyP95Ms = latencyP95Ms,
            cameraAvailable = cameraAvailable,
        )
    }

    private companion object {
        const val PERCENT_SCALE = 100
    }
}
