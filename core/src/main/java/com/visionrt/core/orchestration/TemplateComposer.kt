package com.visionrt.core.orchestration

import com.visionrt.core.domain.Detection
import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity

/**
 * Template-based message generator (ARCHITECTURE §12.4, FR-006, FR-007).
 *
 * Default spoken alerts stay within 6 words, always carry object + direction +
 * proximity when known, and use cautious wording for low confidence. No exact
 * metric distance is ever produced.
 */
object TemplateComposer {

    const val MAX_ALERT_WORDS = 6
    const val CAUTIOUS_MESSAGE = "Possible obstacle."

    fun compose(candidate: AlertCandidate): String =
        compose(candidate.detection, candidate.caution)

    fun compose(detection: Detection, caution: Boolean = false): String {
        if (caution) return CAUTIOUS_MESSAGE
        val label = humanLabel(detection.label)
        val direction = directionPhrase(detection.sector)
        val proximity = proximityPhrase(detection.proximity)
        val parts = listOf(label, proximity, direction)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        return ensurePeriod(capitalizeFirst(parts)).also { message ->
            check(wordCount(message) <= MAX_ALERT_WORDS) {
                "Alert exceeds $MAX_ALERT_WORDS words: $message"
            }
        }
    }

    private fun humanLabel(raw: String): String = raw.trim().ifEmpty { "obstacle" }

    private fun directionPhrase(sector: HorizontalSector): String = when (sector) {
        HorizontalSector.LEFT -> "on left"
        HorizontalSector.RIGHT -> "on right"
        HorizontalSector.CENTER -> "ahead"
    }

    private fun proximityPhrase(proximity: Proximity): String = when (proximity) {
        Proximity.NEAR -> "near"
        Proximity.MEDIUM -> "medium"
        Proximity.FAR -> "far"
        Proximity.UNKNOWN -> ""
    }

    private fun ensurePeriod(text: String): String =
        if (text.endsWith(".")) text else "$text."

    private fun capitalizeFirst(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
}
