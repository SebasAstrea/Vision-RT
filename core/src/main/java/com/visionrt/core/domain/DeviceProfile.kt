package com.visionrt.core.domain

/**
 * Operating profile selected at startup (OR-001). Low-End enforces tighter
 * FPS/resolution/VLM defaults on 4 GB class hardware.
 */
enum class DeviceProfile(val key: String) {
    LOW_END("low_end"),
    STANDARD("standard");

    companion object {
        fun fromKey(key: String): DeviceProfile =
            entries.firstOrNull { it.key == key } ?: LOW_END
    }
}

/**
 * Pure classifier for OR-001 AC1–AC2. JVM-testable; Android callers supply
 * RAM size and entry-level SoC hints (e.g. DeviceRegistry match).
 */
object DeviceProfileClassifier {

    /** OR-001.2: devices at or below this RAM count are Low-End by default. */
    const val LOW_END_MAX_RAM_GB = 4

    private const val LOW_END_INTERVAL_MS = 125L
    private const val STANDARD_INTERVAL_MS = 33L

    /**
     * @param totalRamGb device total RAM in GiB (rounded down from bytes)
     * @param isEntryLevelSoc entry-level / known low-end SoC heuristic
     * @param benchmarkJustifiesStandard set true only with M7-style evidence
     */
    fun classify(
        totalRamGb: Int,
        isEntryLevelSoc: Boolean,
        benchmarkJustifiesStandard: Boolean = false,
    ): DeviceProfile = when {
        benchmarkJustifiesStandard -> DeviceProfile.STANDARD
        totalRamGb <= LOW_END_MAX_RAM_GB || isEntryLevelSoc -> DeviceProfile.LOW_END
        else -> DeviceProfile.STANDARD
    }

    /** Continuous inference interval for OR-001.3c (≤ 8 FPS on Low-End). */
    fun frameIntervalMs(profile: DeviceProfile): Long = when (profile) {
        DeviceProfile.LOW_END -> LOW_END_INTERVAL_MS
        DeviceProfile.STANDARD -> STANDARD_INTERVAL_MS
    }
}

/**
 * Read-only view of the profile computed once at startup (OR-001.4:
 * visible in settings for debugging/support).
 */
interface DeviceProfileProvider {
    val profile: DeviceProfile
}
