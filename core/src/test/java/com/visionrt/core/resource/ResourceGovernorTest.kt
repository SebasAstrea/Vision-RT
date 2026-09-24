package com.visionrt.core.resource

import com.visionrt.core.orchestration.fake.FakeFeedbackPort
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceGovernorTest {

    private fun governor(baseMs: Long = 125L) = ResourceGovernor(baseMs)

    private fun hold(g: ResourceGovernor, s: ResourceSignals, times: Int = ResourceGovernor.HOLD_TICKS) {
        repeat(times) { g.evaluate(s) }
    }

    @Test
    fun startsNormalAtBaseInterval() {
        val g = governor(125L)
        assertEquals(DegradationLevel.NORMAL, g.level)
        assertEquals(125L, g.frameIntervalMs())
        assertFalse(g.continuousDetectionStopped())
        assertFalse(g.optionalModelsDisabled())
    }

    @Test
    fun thermalModerateEscalatesToReducedAndHalvesFps() {
        val g = governor(100L)
        hold(g, ResourceSignals(thermalStatus = ResourceSignals.THERMAL_MODERATE))
        assertEquals(DegradationLevel.REDUCED, g.level)
        assertEquals(ResourceCause.THERMAL, g.lastCause)
        assertEquals(200L, g.frameIntervalMs())
        assertTrue(g.optionalModelsDisabled())
        assertFalse(g.continuousDetectionStopped())
    }

    @Test
    fun thermalSevereStopsContinuousDetection() {
        val g = governor()
        hold(g, ResourceSignals(thermalStatus = ResourceSignals.THERMAL_SEVERE))
        assertEquals(DegradationLevel.CRITICAL, g.level)
        assertTrue(g.continuousDetectionStopped())
        assertEquals(ResourceCause.THERMAL, g.lastCause)
    }

    @Test
    fun thermalCriticalStopsContinuousDetection() {
        val g = governor()
        hold(g, ResourceSignals(thermalStatus = ResourceSignals.THERMAL_CRITICAL))
        assertEquals(DegradationLevel.CRITICAL, g.level)
        assertTrue(g.continuousDetectionStopped())
    }

    @Test
    fun batterySaverCapsIntervalAt3Fps() {
        val g = governor(33L)
        hold(g, ResourceSignals(batterySaverActive = true))
        assertEquals(DegradationLevel.REDUCED, g.level)
        assertEquals(ResourceCause.BATTERY_SAVER, g.lastCause)
        // OR-007.2: FPS ≤ 3 → interval ≥ 333 ms regardless of base × multiplier.
        assertTrue(g.frameIntervalMs() >= ResourceGovernor.BATTERY_SAVER_MIN_INTERVAL_MS)
        assertEquals(ResourceGovernor.BATTERY_SAVER_MIN_INTERVAL_MS, g.frameIntervalMs())
    }

    @Test
    fun memoryCriticalYieldsMinimal() {
        val g = governor()
        hold(g, ResourceSignals(memoryPeakMb = 760.0))
        assertEquals(DegradationLevel.MINIMAL, g.level)
        assertEquals(ResourceCause.MEMORY, g.lastCause)
        assertTrue(g.optionalModelsDisabled())
    }

    @Test
    fun memoryWarnYieldsReduced() {
        val g = governor()
        hold(g, ResourceSignals(memoryPeakMb = 660.0))
        assertEquals(DegradationLevel.REDUCED, g.level)
        assertEquals(ResourceCause.MEMORY, g.lastCause)
    }

    @Test
    fun latencyOverBudgetYieldsReduced() {
        val g = governor()
        hold(g, ResourceSignals(latencyP95Ms = 500.0))
        assertEquals(DegradationLevel.REDUCED, g.level)
        assertEquals(ResourceCause.LATENCY, g.lastCause)
    }

    @Test
    fun cameraLostIsCritical() {
        val g = governor()
        hold(g, ResourceSignals(cameraAvailable = false))
        assertEquals(DegradationLevel.CRITICAL, g.level)
        assertTrue(g.continuousDetectionStopped())
        assertEquals(ResourceCause.CAMERA_LOST, g.lastCause)
    }

    @Test
    fun lowBatteryYieldsReduced() {
        val g = governor()
        hold(g, ResourceSignals(batteryPercent = 10))
        assertEquals(DegradationLevel.REDUCED, g.level)
        assertEquals(ResourceCause.BATTERY_LOW, g.lastCause)
    }

    @Test
    fun recoversToNormalWhenSignalsClear() {
        val g = governor()
        hold(g, ResourceSignals(thermalStatus = ResourceSignals.THERMAL_SEVERE))
        assertEquals(DegradationLevel.CRITICAL, g.level)
        hold(g, ResourceSignals())
        assertEquals(DegradationLevel.NORMAL, g.level)
        assertEquals(ResourceCause.RECOVERED, g.lastCause)
        assertFalse(g.continuousDetectionStopped())
    }

    @Test
    fun singleNoisyTickDoesNotFlap() {
        val g = governor()
        g.evaluate(ResourceSignals(thermalStatus = ResourceSignals.THERMAL_MODERATE))
        assertEquals(DegradationLevel.NORMAL, g.level)
        g.evaluate(ResourceSignals())
        assertEquals(DegradationLevel.NORMAL, g.level)
    }

    @Test
    fun resetReturnsToNormal() {
        val g = governor()
        hold(g, ResourceSignals(thermalStatus = ResourceSignals.THERMAL_SEVERE))
        g.reset()
        assertEquals(DegradationLevel.NORMAL, g.level)
        assertEquals(ResourceCause.NONE, g.lastCause)
        assertEquals(125L, g.frameIntervalMs())
    }
}

class DegradationAnnouncerTest {

    @Test
    fun emitsHeatMessageOncePerTransition() = runTest {
        val feedback = FakeFeedbackPort()
        val announcer = DegradationAnnouncer(feedback, lang = com.visionrt.core.orchestration.AlertLang.EN)
        assertTrue(
            announcer.onGovernorState(DegradationLevel.REDUCED, ResourceCause.THERMAL),
        )
        assertFalse(
            announcer.onGovernorState(DegradationLevel.REDUCED, ResourceCause.THERMAL),
        )
        assertEquals(1, feedback.emitted.size)
        assertEquals("Reduced mode due to device heat.", feedback.emitted.single().message)
    }

    @Test
    fun criticalHeatUsesLimitedDueToHeatCopy() = runTest {
        val feedback = FakeFeedbackPort()
        val announcer = DegradationAnnouncer(feedback, lang = com.visionrt.core.orchestration.AlertLang.EN)
        announcer.onGovernorState(DegradationLevel.CRITICAL, ResourceCause.THERMAL)
        assertEquals(
            "Assistance limited due to device heat.",
            feedback.emitted.single().message,
        )
    }

    @Test
    fun batterySaverAnnounced() = runTest {
        val feedback = FakeFeedbackPort()
        val announcer = DegradationAnnouncer(feedback, lang = com.visionrt.core.orchestration.AlertLang.EN)
        announcer.onGovernorState(DegradationLevel.REDUCED, ResourceCause.BATTERY_SAVER)
        assertEquals(
            "Battery saver active.",
            feedback.emitted.single().message,
        )
    }

    @Test
    fun recoveryAnnouncedInSpanish() = runTest {
        val feedback = FakeFeedbackPort()
        val announcer = DegradationAnnouncer(feedback, lang = com.visionrt.core.orchestration.AlertLang.ES)
        announcer.onGovernorState(DegradationLevel.REDUCED, ResourceCause.THERMAL)
        feedback.clear()
        announcer.onGovernorState(DegradationLevel.NORMAL, ResourceCause.RECOVERED)
        assertEquals(
            "Rendimiento normal restablecido.",
            feedback.emitted.single().message,
        )
    }

    @Test
    fun noAnnouncementWhenUnchanged() = runTest {
        val feedback = FakeFeedbackPort()
        val announcer = DegradationAnnouncer(feedback, lang = com.visionrt.core.orchestration.AlertLang.EN)
        assertFalse(announcer.onGovernorState(DegradationLevel.NORMAL, ResourceCause.NONE))
        assertTrue(feedback.emitted.isEmpty())
    }
}
