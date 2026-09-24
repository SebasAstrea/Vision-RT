package com.visionrt.core.ocr

import com.visionrt.core.orchestration.AlertLang

/**
 * Template-based multi-object summary (FR-008, ARCHITECTURE §18.2).
 *
 * Low-end path: detector classes + direction + proximity only — no VLM.
 * Spoken output stays short (FR-008 concision) and cautious; never claims
 * the scene is clear or safe.
 */
object ObjectSummaryComposer {

    /** Soft cap on spoken words for a single summary utterance. */
    const val MAX_SUMMARY_WORDS = 24

    /** How many distinct detections are named before a catch-all count. */
    const val MAX_NAMED_OBJECTS = 5

    fun emptyMessage(lang: AlertLang = AlertLang.current()): String = when (lang) {
        AlertLang.ES -> "No se detectaron objetos."
        AlertLang.EN -> "No objects detected."
    }

    fun timeoutMessage(lang: AlertLang = AlertLang.current()): String = when (lang) {
        AlertLang.ES -> "La descripción tardó demasiado."
        AlertLang.EN -> "The description took too long."
    }

    fun failureMessage(lang: AlertLang = AlertLang.current()): String = when (lang) {
        AlertLang.ES -> "No se pudo describir la escena."
        AlertLang.EN -> "Could not describe the scene."
    }

    /**
     * Groups detections by label (stable order: count desc, then label),
     * then phrases each group with proximity + direction from its first
     * member. Returns [emptyMessage] when [detections] is empty.
     */
    fun compose(
        detections: List<com.visionrt.core.domain.Detection>,
        lang: AlertLang = AlertLang.current(),
    ): String {
        if (detections.isEmpty()) return emptyMessage(lang)
        val groups = detections
            .groupBy { it.label.trim().lowercase() }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<com.visionrt.core.domain.Detection>>> {
                    it.value.size
                }.thenBy { it.key },
            )
            .take(MAX_NAMED_OBJECTS)

        val phrases = groups.map { (_, group) ->
            val first = group.first()
            val label = lang.label(first.label)
            val countPrefix = when {
                group.size > 1 && lang == AlertLang.ES -> "${group.size} "
                group.size > 1 && lang == AlertLang.EN -> "${group.size} "
                else -> ""
            }
            val proximity = proximityPhrase(first.proximity, lang)
            val direction = directionPhrase(first.sector, lang)
            listOf(countPrefix + label, proximity, direction)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }

        val overflow = detections.groupBy { it.label.trim().lowercase() }.size - MAX_NAMED_OBJECTS
        val open = when (lang) {
            AlertLang.ES -> "y más"
            AlertLang.EN -> "and more"
        }
        val body = if (overflow > 0) {
            (phrases + open).joinToString(", ")
        } else {
            phrases.joinToString(", ")
        }
        val joined = capitalize(body) + "."
        return if (wordCount(joined) > MAX_SUMMARY_WORDS) {
            truncateToWords(joined, MAX_SUMMARY_WORDS)
        } else {
            joined
        }
    }

    private fun proximityPhrase(
        proximity: com.visionrt.core.domain.Proximity,
        lang: AlertLang,
    ): String = when (lang) {
        AlertLang.ES -> when (proximity) {
            com.visionrt.core.domain.Proximity.NEAR -> "cerca"
            com.visionrt.core.domain.Proximity.MEDIUM -> "media"
            com.visionrt.core.domain.Proximity.FAR -> "lejos"
            com.visionrt.core.domain.Proximity.UNKNOWN -> ""
        }
        AlertLang.EN -> when (proximity) {
            com.visionrt.core.domain.Proximity.NEAR -> "near"
            com.visionrt.core.domain.Proximity.MEDIUM -> "medium"
            com.visionrt.core.domain.Proximity.FAR -> "far"
            com.visionrt.core.domain.Proximity.UNKNOWN -> ""
        }
    }

    private fun directionPhrase(
        sector: com.visionrt.core.domain.HorizontalSector,
        lang: AlertLang,
    ): String = when (lang) {
        AlertLang.ES -> when (sector) {
            com.visionrt.core.domain.HorizontalSector.LEFT -> "a la izquierda"
            com.visionrt.core.domain.HorizontalSector.RIGHT -> "a la derecha"
            com.visionrt.core.domain.HorizontalSector.CENTER -> "delante"
        }
        AlertLang.EN -> when (sector) {
            com.visionrt.core.domain.HorizontalSector.LEFT -> "on left"
            com.visionrt.core.domain.HorizontalSector.RIGHT -> "on right"
            com.visionrt.core.domain.HorizontalSector.CENTER -> "ahead"
        }
    }

    private fun capitalize(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    private fun truncateToWords(text: String, max: Int): String {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val cut = words.take(max).joinToString(" ").trimEnd('.', ',', ' ')
        return "$cut…"
    }
}
