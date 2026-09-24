package com.visionrt.app.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.visionrt.benchmark.DeviceRegistry
import com.visionrt.core.common.SafeLogger
import com.visionrt.core.domain.DeviceProfile
import com.visionrt.core.domain.DeviceProfileClassifier
import com.visionrt.core.domain.DeviceProfileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OR-001 startup classification: total RAM + DeviceRegistry entry-level hint →
 * Low-End / Standard. Computed once; shown in settings (AC4) and used to
 * enforce FPS (AC3).
 */
@Singleton
class AndroidDeviceProfileProvider @Inject constructor(
    @ApplicationContext context: Context,
) : DeviceProfileProvider {

    override val profile: DeviceProfile = classify(context)

    private fun classify(context: Context): DeviceProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val ramGb = (info.totalMem / BYTES_PER_GB).toInt().coerceAtLeast(1)
        val registry = DeviceRegistry.KNOWN.firstOrNull { it.model == Build.MODEL }
        val entryLevelSoc = registry != null &&
            registry.ramGb <= DeviceProfileClassifier.LOW_END_MAX_RAM_GB
        val selected = DeviceProfileClassifier.classify(
            totalRamGb = ramGb,
            isEntryLevelSoc = entryLevelSoc,
        )
        SafeLogger.i(
            TAG,
            "profile=${selected.key} ramGb=$ramGb model=${Build.MODEL} " +
                "registryEntry=${registry != null}",
        )
        return selected
    }

    private companion object {
        const val TAG = "DeviceProfile"
        const val BYTES_PER_GB = 1_073_741_824L
    }
}
