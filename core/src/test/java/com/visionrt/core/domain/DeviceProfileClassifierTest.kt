package com.visionrt.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceProfileClassifierTest {

    @Test
    fun fourGbRamIsLowEnd() {
        assertEquals(
            DeviceProfile.LOW_END,
            DeviceProfileClassifier.classify(totalRamGb = 4, isEntryLevelSoc = false),
        )
    }

    @Test
    fun entryLevelSocIsLowEndEvenWithMoreRam() {
        assertEquals(
            DeviceProfile.LOW_END,
            DeviceProfileClassifier.classify(totalRamGb = 6, isEntryLevelSoc = true),
        )
    }

    @Test
    fun sixGbNonEntryIsStandard() {
        assertEquals(
            DeviceProfile.STANDARD,
            DeviceProfileClassifier.classify(totalRamGb = 6, isEntryLevelSoc = false),
        )
    }

    @Test
    fun benchmarkEvidenceOverridesToStandard() {
        assertEquals(
            DeviceProfile.STANDARD,
            DeviceProfileClassifier.classify(
                totalRamGb = 4,
                isEntryLevelSoc = true,
                benchmarkJustifiesStandard = true,
            ),
        )
    }

    @Test
    fun lowEndFrameIntervalCapsAtEightFps() {
        val ms = DeviceProfileClassifier.frameIntervalMs(DeviceProfile.LOW_END)
        assertEquals(125L, ms)
        // ≤ 8 FPS means interval ≥ 1000/8 = 125 ms
        assert(ms * 8 >= 1000L)
    }

    @Test
    fun standardFrameIntervalAllowsHigherFps() {
        assertEquals(33L, DeviceProfileClassifier.frameIntervalMs(DeviceProfile.STANDARD))
    }
}
