package com.visionrt.core.orchestration

import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity
import com.visionrt.core.orchestration.fake.FakeDetectionSource
import com.visionrt.core.orchestration.fake.FakeDetections
import com.visionrt.core.orchestration.fake.FakeFeedbackPort
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M3 exit criteria: detection results reach AlertPolicy through AlertPipeline
 * (integration with fakes for the policy path; camera/LiteRT covered by
 * unit tests + on-device pass).
 */
class DetectionsReachAlertPolicyTest {

    @Test
    fun persistentObstacleProducesCriticalAlert() = runTest {
        val feedback = FakeFeedbackPort()
        val policy = AlertPolicy()
        var now = 1_000L
        val pipeline = AlertPipeline(policy, feedback, clock = { now })
        val source = FakeDetectionSource()

        val scene = listOf(
            FakeDetections.of(
                label = "chair",
                confidence = 0.92f,
                sector = HorizontalSector.CENTER,
                proximity = Proximity.NEAR,
            ),
        )

        // minStableFrames = 2: first frame arms the track, second fires.
        repeat(3) { frame ->
            now = 1_000L + frame * 100L
            source.push(scene)
            pipeline.onFrame(source.detect().getOrThrow())
        }

        assertTrue(
            "FeedbackPort received alerts",
            feedback.emitted.isNotEmpty(),
        )
        assertTrue(
            "Priority should be CRITICAL_OBSTACLE",
            feedback.emitted.all { it.priority == AlertPriority.CRITICAL_OBSTACLE },
        )
        val message = feedback.emitted.first().message.lowercase()
        assertTrue("Message should mention object: $message", message.contains("chair"))
        assertTrue(
            "No unsafe claim",
            !message.contains("safe path") && !message.contains("walk now"),
        )
    }
}

