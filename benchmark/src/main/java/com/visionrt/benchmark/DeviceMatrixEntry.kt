package com.visionrt.benchmark

/**
 * Registry entry for one low-end reference device (see docs/DEVICE_PROCUREMENT.md).
 * Used by the benchmark screens (M7) to replay workloads per hardware profile.
 */
data class DeviceMatrixEntry(
    val model: String,
    val soC: String,
    val ramGb: Int,
    val androidSdk: Int,
    val notes: String = "",
)

/**
 * Devices physically available as QA/benchmark instruments.
 * See docs/DEVICE_PROCUREMENT.md.
 */
object DeviceRegistry {
    val KNOWN: List<DeviceMatrixEntry> = listOf(
        DeviceMatrixEntry(
            model = "SM-A226BR",
            soC = "MediaTek Dimensity 700 (MT6833)",
            ramGb = 4,
            androidSdk = 33,
            notes = "Galaxy A22 5G; reference low-end unit. QA instrument #1 (USB)."
        )
    )
}
