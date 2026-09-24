package com.visionrt.core.orchestration

import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity
import com.visionrt.core.domain.Verbosity
import com.visionrt.core.orchestration.fake.FakeDetections
import com.visionrt.core.orchestration.fake.FakeDetectionSource
import com.visionrt.core.orchestration.fake.FakeFeedbackPort
import com.visionrt.core.orchestration.fake.FakeOcrSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeControllerTest {

    private val feedback = FakeFeedbackPort()
    private var nowMs = 1_000L
    private val controller = ModeController(feedback, { nowMs }, AlertLang.EN)

    @Test
    fun startsIdle() {
        assertEquals(OrchestrationState.IDLE, controller.current)
        assertTrue(feedback.emitted.isEmpty())
    }

    @Test
    fun happyPathToObstacleAssistance() = runTest {
        assertTrue(controller.start())
        assertEquals(OrchestrationState.STARTING, controller.current)
        assertTrue(controller.onReady())
        assertEquals(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE, controller.current)
        assertTrue(feedback.messages().contains("Starting assistance."))
        assertTrue(feedback.messages().contains("Obstacle assistance ready."))
    }

    @Test
    fun rejectsIllegalTransitionAndKeepsState() = runTest {
        assertFalse(controller.onReady())
        assertEquals(OrchestrationState.IDLE, controller.current)
        assertFalse(controller.enterTextReading())
        assertEquals(OrchestrationState.IDLE, controller.current)
    }

    @Test
    fun objectQueryRoundTrip() = runTest {
        controller.start()
        controller.onReady()
        assertTrue(controller.enterObjectQuery())
        assertEquals(OrchestrationState.OBJECT_QUERY_ACTIVE, controller.current)
        assertTrue(controller.returnToObstacleAssistance())
        assertEquals(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE, controller.current)
    }

    @Test
    fun textReadingRoundTrip() = runTest {
        controller.start()
        controller.onReady()
        assertTrue(controller.enterTextReading())
        assertEquals(OrchestrationState.TEXT_READING_ACTIVE, controller.current)
        assertTrue(controller.returnToObstacleAssistance())
        assertEquals(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE, controller.current)
    }

    @Test
    fun degradeAndRecover() = runTest {
        controller.start()
        controller.onReady()
        assertTrue(controller.degrade())
        assertEquals(OrchestrationState.DEGRADED, controller.current)
        assertTrue(feedback.messages().contains("Assistance is limited."))
        assertTrue(controller.returnToObstacleAssistance())
        assertEquals(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE, controller.current)
    }

    @Test
    fun degradeToError() = runTest {
        controller.start()
        controller.onReady()
        controller.degrade()
        assertTrue(controller.transition(OrchestrationState.ERROR))
        assertEquals(OrchestrationState.ERROR, controller.current)
    }

    @Test
    fun stopFromAnyActiveStateReturnsIdle() = runTest {
        controller.start()
        assertTrue(controller.stopSession())
        assertEquals(OrchestrationState.IDLE, controller.current)
        assertEquals(1, feedback.stopAllCount)
        assertTrue(feedback.messages().contains("Assistance stopped."))
    }

    @Test
    fun stopFromErrorAllowed() = runTest {
        controller.start()
        controller.transition(OrchestrationState.ERROR)
        assertEquals(OrchestrationState.ERROR, controller.current)
        assertTrue(controller.stopSession())
        assertEquals(OrchestrationState.IDLE, controller.current)
    }

    @Test
    fun everyDocumentedTargetIsReachableWhenLegal() = runTest {
        controller.start()
        controller.onReady()
        val targets = OrchestrationState.targets(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE)
        assertTrue(targets.contains(OrchestrationState.OBJECT_QUERY_ACTIVE))
        assertTrue(targets.contains(OrchestrationState.TEXT_READING_ACTIVE))
        assertTrue(targets.contains(OrchestrationState.DEGRADED))
        assertTrue(targets.contains(OrchestrationState.ERROR))
        assertTrue(targets.contains(OrchestrationState.STOPPING))
        assertTrue(controller.transition(OrchestrationState.OBJECT_QUERY_ACTIVE))
        assertEquals(OrchestrationState.OBJECT_QUERY_ACTIVE, controller.current)
        assertTrue(controller.returnToObstacleAssistance())
        assertTrue(controller.transition(OrchestrationState.TEXT_READING_ACTIVE))
        assertEquals(OrchestrationState.TEXT_READING_ACTIVE, controller.current)
        assertTrue(controller.returnToObstacleAssistance())
        assertTrue(controller.transition(OrchestrationState.DEGRADED))
        assertEquals(OrchestrationState.DEGRADED, controller.current)
        assertTrue(controller.returnToObstacleAssistance())
        assertEquals(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE, controller.current)
        assertTrue(controller.transition(OrchestrationState.ERROR))
        assertEquals(OrchestrationState.ERROR, controller.current)
        assertTrue(controller.transition(OrchestrationState.STOPPING))
        assertEquals(OrchestrationState.STOPPING, controller.current)
        assertTrue(controller.transition(OrchestrationState.IDLE))
        assertEquals(OrchestrationState.IDLE, controller.current)
    }

    @Test
    fun stateAnnouncementsAreNeverSilent() = runTest {
        controller.start()
        controller.onReady()
        controller.enterObjectQuery()
        controller.returnToObstacleAssistance()
        controller.enterTextReading()
        controller.returnToObstacleAssistance()
        controller.degrade()
        controller.returnToObstacleAssistance()
        controller.transition(OrchestrationState.ERROR)
        controller.stopSession()
        assertTrue(feedback.emitted.isNotEmpty())
        assertTrue(feedback.emitted.all { it.message.isNotBlank() })
        assertTrue(feedback.emitted.all { it.message.endsWith(".") })
    }
}

class AlertPolicyTest {

    private val policy = AlertPolicy(
        AlertPolicyConfig(
            confidenceThreshold = 0.60f,
            highConfidenceThreshold = 0.80f,
            minStableFrames = 2,
            cooldownMs = 5_000L,
            cautionConfidenceThreshold = 0.70f,
        ),
    )

    @Test
    fun belowConfidenceThresholdIsIgnored() {
        val low = FakeDetections.of("person", confidence = 0.50f)
        assertTrue(policy.evaluate(listOf(low), nowMs = 0).isEmpty())
        assertTrue(policy.evaluate(listOf(low), nowMs = 100).isEmpty())
    }

    @Test
    fun singleFrameDoesNotAlertWithoutMotionEvidence() {
        val person = FakeDetections.of("person", confidence = 0.90f)
        assertTrue(policy.evaluate(listOf(person), nowMs = 0).isEmpty())
    }

    @Test
    fun nearHighConfidenceAlertsOnFirstFrame() {
        val person = FakeDetections.of(
            "person",
            confidence = 0.90f,
            proximity = Proximity.NEAR,
        )
        assertEquals(1, policy.evaluate(listOf(person), nowMs = 0).size)
    }

    @Test
    fun nearButBelowHighConfidenceNeedsStableFrames() {
        val person = FakeDetections.of(
            "person",
            confidence = 0.75f,
            proximity = Proximity.NEAR,
        )
        assertTrue(policy.evaluate(listOf(person), nowMs = 0).isEmpty())
        assertEquals(1, policy.evaluate(listOf(person), nowMs = 100).size)
    }

    @Test
    fun twoConsecutiveFramesProduceCandidate() {
        val person = FakeDetections.of("person", confidence = 0.90f)
        assertTrue(policy.evaluate(listOf(person), nowMs = 0).isEmpty())
        val second = policy.evaluate(listOf(person), nowMs = 100)
        assertEquals(1, second.size)
        assertFalse(second[0].caution)
    }

    @Test
    fun highConfidencePlusMotionAlertsOnFirstFrame() {
        val person = FakeDetections.of("person", confidence = 0.85f)
        val result = policy.evaluate(listOf(person), nowMs = 0, motionEvidence = true)
        assertEquals(1, result.size)
    }

    @Test
    fun gapInFramesResetsPersistence() {
        val person = FakeDetections.of("person", confidence = 0.90f)
        policy.evaluate(listOf(person), nowMs = 0)
        policy.evaluate(emptyList(), nowMs = 50)
        assertTrue(policy.evaluate(listOf(person), nowMs = 100).isEmpty())
        assertEquals(1, policy.evaluate(listOf(person), nowMs = 150).size)
    }

    @Test
    fun cooldownSuppressesRepeatOfSameObject() {
        val person = FakeDetections.of("person", confidence = 0.90f)
        policy.evaluate(listOf(person), nowMs = 0)
        assertEquals(1, policy.evaluate(listOf(person), nowMs = 100).size)
        policy.evaluate(listOf(person), nowMs = 200)
        val withinCooldown = policy.evaluate(listOf(person), nowMs = 300)
        assertTrue(withinCooldown.isEmpty())
    }

    @Test
    fun cooldownExpiresAfterConfiguredWindow() {
        val person = FakeDetections.of("person", confidence = 0.90f)
        policy.evaluate(listOf(person), nowMs = 0)
        assertEquals(1, policy.evaluate(listOf(person), nowMs = 100).size)
        policy.evaluate(listOf(person), nowMs = 200)
        val afterCooldown = policy.evaluate(listOf(person), nowMs = 100 + 5_000L)
        assertEquals(1, afterCooldown.size)
    }

    @Test
    fun proximityIncreaseBypassesCooldown() {
        val far = FakeDetections.of(
            "person",
            confidence = 0.90f,
            proximity = Proximity.FAR,
        )
        policy.evaluate(listOf(far), nowMs = 0)
        assertEquals(1, policy.evaluate(listOf(far), nowMs = 100).size)
        val near = FakeDetections.of(
            "person",
            confidence = 0.90f,
            proximity = Proximity.NEAR,
        )
        val riskUp = policy.evaluate(listOf(near), nowMs = 200)
        assertEquals(1, riskUp.size)
    }

    @Test
    fun lowStableConfidenceMarksCaution() {
        val person = FakeDetections.of("person", confidence = 0.65f)
        policy.evaluate(listOf(person), nowMs = 0)
        val second = policy.evaluate(listOf(person), nowMs = 100)
        assertEquals(1, second.size)
        assertTrue(second[0].caution)
    }

    @Test
    fun differentSectorsTrackIndependently() {
        val left = FakeDetections.of(
            "chair",
            confidence = 0.90f,
            sector = HorizontalSector.LEFT,
        )
        val right = FakeDetections.of(
            "chair",
            confidence = 0.90f,
            sector = HorizontalSector.RIGHT,
        )
        policy.evaluate(listOf(left, right), nowMs = 0)
        assertEquals(2, policy.evaluate(listOf(left, right), nowMs = 100).size)
    }

    @Test
    fun detailedCooldownAllowsReAlertSoonerThanNormal() {
        val person = FakeDetections.of("person", confidence = 0.90f)
        policy.evaluate(listOf(person), nowMs = 0)
        assertEquals(1, policy.evaluate(listOf(person), nowMs = 100).size)
        val detailedAfter = policy.evaluate(
            listOf(person),
            nowMs = 2_100L,
            verbosity = Verbosity.DETAILED,
        )
        assertEquals(1, detailedAfter.size)
        policy.evaluate(listOf(person), nowMs = 2_200L, verbosity = Verbosity.DETAILED)
        val detailedWithin = policy.evaluate(
            listOf(person),
            nowMs = 2_300L,
            verbosity = Verbosity.DETAILED,
        )
        assertTrue(detailedWithin.isEmpty())
    }

    @Test
    fun resetClearsAllTracks() {
        val person = FakeDetections.of("person", confidence = 0.90f)
        policy.evaluate(listOf(person), nowMs = 0)
        policy.evaluate(listOf(person), nowMs = 100)
        policy.reset()
        assertTrue(policy.evaluate(listOf(person), nowMs = 200).isEmpty())
    }
}

class TemplateComposerTest {

    @Test
    fun personAheadMatchesArchitectureExample() {
        val detection = FakeDetections.of(
            "person",
            sector = HorizontalSector.CENTER,
            proximity = Proximity.MEDIUM,
        )
        assertEquals("Person medium ahead.", TemplateComposer.compose(detection, lang = AlertLang.EN))
    }

    @Test
    fun chairOnLeftMatchesArchitectureExample() {
        val detection = FakeDetections.of(
            "chair",
            sector = HorizontalSector.LEFT,
            proximity = Proximity.UNKNOWN,
        )
        assertEquals("Chair on left.", TemplateComposer.compose(detection, lang = AlertLang.EN))
    }

    @Test
    fun cautiousMessageIsPossibleObstacle() {
        assertEquals("Possible obstacle.", TemplateComposer.CAUTIOUS_MESSAGE)
        val detection = FakeDetections.of("person", confidence = 0.65f)
        assertEquals(
            "Possible obstacle.",
            TemplateComposer.compose(detection, caution = true, lang = AlertLang.EN),
        )
    }

    @Test
    fun nearObstacleCenter() {
        val detection = FakeDetections.of(
            "obstacle",
            sector = HorizontalSector.CENTER,
            proximity = Proximity.NEAR,
        )
        assertEquals("Obstacle near ahead.", TemplateComposer.compose(detection, lang = AlertLang.EN))
    }

    @Test
    fun rightSectorPhrasing() {
        val detection = FakeDetections.of(
            "door",
            sector = HorizontalSector.RIGHT,
            proximity = Proximity.FAR,
        )
        assertEquals("Door far on right.", TemplateComposer.compose(detection, lang = AlertLang.EN))
    }

    @Test
    fun spanishPersonNearCenterUsesSpanishLabels() {
        val detection = FakeDetections.of(
            "person",
            sector = HorizontalSector.CENTER,
            proximity = Proximity.NEAR,
        )
        assertEquals(
            "Persona cerca delante.",
            TemplateComposer.compose(detection, lang = AlertLang.ES),
        )
    }

    @Test
    fun spanishCautiousIsPosibleObstaculo() {
        assertEquals("Posible obstáculo.", TemplateComposer.cautiousMessage(AlertLang.ES))
    }

    @Test
    fun defaultAlertNeverExceedsSixWords() {
        val samples = listOf(
            FakeDetections.of("person", sector = HorizontalSector.CENTER, proximity = Proximity.NEAR),
            FakeDetections.of("bicycle", sector = HorizontalSector.LEFT, proximity = Proximity.MEDIUM),
            FakeDetections.of("generic obstacle", sector = HorizontalSector.RIGHT, proximity = Proximity.FAR),
        )
        AlertLang.entries.forEach { lang ->
            samples.forEach { detection ->
                val words = TemplateComposer.compose(detection, lang = lang)
                    .trim('.')
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .size
                assertTrue("too many words ($lang): $words", words <= TemplateComposer.MAX_ALERT_WORDS)
            }
        }
    }

    @Test
    fun emptyLabelFallsBackToObstacle() {
        val detection = FakeDetections.of(
            "  ",
            sector = HorizontalSector.CENTER,
            proximity = Proximity.UNKNOWN,
        )
        assertEquals("Obstacle ahead.", TemplateComposer.compose(detection, lang = AlertLang.EN))
    }

    @Test
    fun composeFromCandidateUsesCautionFlag() {
        val detection = FakeDetections.of("person", confidence = 0.65f)
        val candidate = AlertCandidate(
            detection = detection,
            priority = com.visionrt.core.domain.AlertPriority.CRITICAL_OBSTACLE,
            caution = true,
        )
        assertEquals("Possible obstacle.", TemplateComposer.compose(candidate, lang = AlertLang.EN))
    }
}

class AlertPipelineTest {

    private val feedback = FakeFeedbackPort()
    private var nowMs = 0L
    private var idSeq = 0L
    private val policy = AlertPolicy()
    private val pipeline = AlertPipeline(
        policy = policy,
        feedback = feedback,
        clock = { nowMs },
        idSource = { ++idSeq },
        lang = AlertLang.EN,
    )

    @Test
    fun criticalAlertInterruptsLowerPriorityThenEmits() = runTest {
        feedback.emit(
            com.visionrt.core.domain.Alert(
                id = "status-1",
                priority = com.visionrt.core.domain.AlertPriority.STATUS,
                message = "Obstacle assistance active.",
                createdAtMs = nowMs,
            ),
        )
        val person = FakeDetections.of("person", confidence = 0.90f)
        nowMs = 10
        pipeline.onFrame(listOf(person))
        nowMs = 20
        val alerts = pipeline.onFrame(listOf(person))
        assertEquals(1, alerts.size)
        assertTrue(
            feedback.interrupts.contains(com.visionrt.core.domain.AlertPriority.CRITICAL_OBSTACLE),
        )
        assertEquals("Person medium ahead.", alerts[0].message)
        assertEquals(com.visionrt.core.domain.AlertPriority.CRITICAL_OBSTACLE, alerts[0].priority)
    }

    @Test
    fun minimalVerbosityStillEmitsCriticalObstacles() = runTest {
        val person = FakeDetections.of("person", confidence = 0.90f)
        nowMs = 10
        pipeline.onFrame(listOf(person), verbosity = Verbosity.MINIMAL)
        nowMs = 20
        val alerts = pipeline.onFrame(listOf(person), verbosity = Verbosity.MINIMAL)
        assertEquals(1, alerts.size)
        assertTrue(feedback.emitted.any { it.message == "Person medium ahead." })
    }

    @Test
    fun detailedVerbosityReAlertsAfterShortCooldown() = runTest {
        val person = FakeDetections.of("person", confidence = 0.90f)
        nowMs = 10
        pipeline.onFrame(listOf(person), verbosity = Verbosity.DETAILED)
        nowMs = 20
        assertEquals(1, pipeline.onFrame(listOf(person), verbosity = Verbosity.DETAILED).size)
        nowMs = 2_100L
        assertEquals(1, pipeline.onFrame(listOf(person), verbosity = Verbosity.DETAILED).size)
    }

    @Test
    fun normalVerbosityStillCooldownAtDetailedBoundary() = runTest {
        val person = FakeDetections.of("person", confidence = 0.90f)
        nowMs = 10
        pipeline.onFrame(listOf(person), verbosity = Verbosity.NORMAL)
        nowMs = 20
        assertEquals(1, pipeline.onFrame(listOf(person), verbosity = Verbosity.NORMAL).size)
        nowMs = 2_100L
        assertTrue(pipeline.onFrame(listOf(person), verbosity = Verbosity.NORMAL).isEmpty())
    }

    @Test
    fun noAlertOnFirstFrameAlone() = runTest {
        val person = FakeDetections.of("person", confidence = 0.90f)
        val alerts = pipeline.onFrame(listOf(person))
        assertTrue(alerts.isEmpty())
        assertTrue(feedback.emitted.none { it.priority.name == "CRITICAL_OBSTACLE" })
    }

    @Test
    fun failedDetectionResultDoesNotThrow() = runTest {
        val source = FakeDetectionSource()
        source.failNextDetect()
        val result = source.detect()
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }
}

class OrchestrationIntegrationTest {

    @Test
    fun fullAlertPathFromDetectionToFeedback() = runTest {
        val feedback = FakeFeedbackPort()
        val detector = FakeDetectionSource()
        val ocr = FakeOcrSource()
        var nowMs = 0L
        var idSeq = 0L
        val policy = AlertPolicy()
        val pipeline = AlertPipeline(policy, feedback, { nowMs }, { ++idSeq }, AlertLang.EN)
        val modes = ModeController(feedback, { nowMs }, AlertLang.EN)

        assertTrue(modes.start())
        assertTrue(modes.onReady())
        assertEquals(OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE, modes.current)

        val person = FakeDetections.of(
            "person",
            confidence = 0.90f,
            sector = HorizontalSector.CENTER,
            proximity = Proximity.NEAR,
        )

        detector.push(listOf(person))
        val frame1 = detector.detect().getOrThrow()
        nowMs = 100
        val alerts = pipeline.onFrame(frame1)
        assertEquals(1, alerts.size)
        assertEquals("Person near ahead.", alerts[0].message)
        assertTrue(feedback.emitted.any { it.message == "Person near ahead." })
        assertTrue(feedback.interrupts.isNotEmpty())

        nowMs = 300
        detector.push(listOf(person))
        val frame2 = detector.detect().getOrThrow()
        assertTrue(pipeline.onFrame(frame2).isEmpty())

        nowMs = 100 + 5_000L
        detector.push(listOf(person))
        val frame3 = detector.detect().getOrThrow()
        assertEquals(1, pipeline.onFrame(frame3).size)

        ocr.stageText("EXIT")
        assertEquals(Result.success("EXIT"), ocr.recognize())

        assertTrue(modes.transition(OrchestrationState.STOPPING))
        feedback.stopAll()
        assertTrue(modes.transition(OrchestrationState.IDLE))
        assertEquals(OrchestrationState.IDLE, modes.current)
        assertTrue(feedback.messages().contains("Assistance stopped."))
        assertTrue(feedback.emitted.isNotEmpty())
    }

    @Test
    fun spanishLocaleAnnouncesInSpanish() = runTest {
        val feedback = FakeFeedbackPort()
        var nowMs = 0L
        val modes = ModeController(feedback, { nowMs }, AlertLang.ES)
        assertTrue(modes.start())
        assertTrue(modes.onReady())
        assertTrue(feedback.messages().contains("Iniciando asistencia."))
        assertTrue(feedback.messages().contains("Asistencia de obstáculos lista."))
    }

    @Test
    fun criticalAlertInterruptsNonCriticalFeedback() = runTest {
        val feedback = FakeFeedbackPort()
        val policy = AlertPolicy()
        var nowMs = 0L
        var idSeq = 0L
        val pipeline = AlertPipeline(policy, feedback, { nowMs }, { ++idSeq }, AlertLang.EN)

        feedback.emit(
            com.visionrt.core.domain.Alert(
                id = "ocr-1",
                priority = com.visionrt.core.domain.AlertPriority.OCR_RESULT,
                message = "Some text result.",
                createdAtMs = 0,
            ),
        )
        assertEquals(
            com.visionrt.core.domain.AlertPriority.OCR_RESULT,
            feedback.currentPriority,
        )

        val person = FakeDetections.of("person", confidence = 0.90f)
        nowMs = 10
        pipeline.onFrame(listOf(person))
        nowMs = 20
        pipeline.onFrame(listOf(person))

        assertTrue(feedback.interrupts.isNotEmpty())
        assertEquals(
            com.visionrt.core.domain.AlertPriority.CRITICAL_OBSTACLE,
            feedback.currentPriority,
        )
    }

    @Test
    fun latestOnlyChannelDropsStaleItems() {
        val channel = LatestOnlyChannel<Int>()
        channel.submit(1)
        channel.submit(2)
        channel.submit(3)
        assertEquals(3, channel.tryConsume())
        assertNull(channel.tryConsume())
        assertNull(channel.peek())
    }
}
