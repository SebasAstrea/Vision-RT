package com.visionrt.core.ocr

import com.visionrt.core.domain.Detection
import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity
import com.visionrt.core.orchestration.AlertLang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectSummaryComposerTest {

    private fun det(
        label: String,
        sector: HorizontalSector = HorizontalSector.CENTER,
        proximity: Proximity = Proximity.NEAR,
    ) = Detection(
        id = "d-$label",
        classId = 0,
        label = label,
        confidence = 0.9f,
        boundingBox = com.visionrt.core.domain.BoundingBox(0.1f, 0.1f, 0.4f, 0.4f),
        sector = sector,
        proximity = proximity,
        timestampMs = 0L,
        sourceModelVersion = "test",
    )

    @Test
    fun emptyDetectionsReturnsLocalizedEmptyMessage() {
        assertEquals("No objects detected.", ObjectSummaryComposer.compose(emptyList(), AlertLang.EN))
        assertEquals("No se detectaron objetos.", ObjectSummaryComposer.compose(emptyList(), AlertLang.ES))
    }

    @Test
    fun singleDetectionPhrasesLabelProximityDirection() {
        val msg = ObjectSummaryComposer.compose(listOf(det("person")), AlertLang.EN)
        assertEquals("Person near ahead.", msg)
    }

    @Test
    fun spanishUsesLocalizedDirectionAndLabel() {
        val msg = ObjectSummaryComposer.compose(
            listOf(det("person", HorizontalSector.LEFT, Proximity.NEAR)),
            AlertLang.ES,
        )
        assertEquals("Persona cerca a la izquierda.", msg)
    }

    @Test
    fun groupsRepeatedLabelsWithCount() {
        val msg = ObjectSummaryComposer.compose(
            listOf(
                det("chair"),
                det("chair"),
                det("person", HorizontalSector.RIGHT),
            ),
            AlertLang.EN,
        )
        assertTrue("msg=$msg", msg.startsWith("2 chair"))
        assertTrue(msg.contains("person"))
        assertTrue(msg.endsWith("."))
    }

    @Test
    fun capsNamedObjectsAndWordCount() {
        val many = (1..10).map { det("object$it") }
        val msg = ObjectSummaryComposer.compose(many, AlertLang.EN)
        val words = msg.trim().split(Regex("\\s+")).size
        assertTrue("words=$words msg=$msg", words <= ObjectSummaryComposer.MAX_SUMMARY_WORDS)
        assertTrue(msg.contains("and more"))
    }

    @Test
    fun cautiousWordingNeverClaimsSafety() {
        val msg = ObjectSummaryComposer.compose(listOf(det("wall")), AlertLang.EN)
        val forbidden = listOf("safe", "clear", "walk", "no obstacles")
        forbidden.forEach { assertFalse(msg.lowercase().contains(it)) }
    }
}
