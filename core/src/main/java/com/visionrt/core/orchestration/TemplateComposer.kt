package com.visionrt.core.orchestration

import com.visionrt.core.domain.Detection
import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity
import java.util.Locale

/**
 * Template-based message generator (ARCHITECTURE §12.4, FR-006, FR-007).
 *
 * Default spoken alerts stay within 6 words, always carry object + direction +
 * proximity when known, and use cautious wording for low confidence. No exact
 * metric distance is ever produced. Messages follow the UI locale so TTS does
 * not mix languages on a Spanish device.
 */
object TemplateComposer {

    const val MAX_ALERT_WORDS = 6

    fun cautiousMessage(lang: AlertLang = AlertLang.current()): String = when (lang) {
        AlertLang.ES -> "Posible obstáculo."
        AlertLang.EN -> "Possible obstacle."
    }

    /** Fixed English constant for legacy call sites/tests. */
    val CAUTIOUS_MESSAGE: String get() = cautiousMessage(AlertLang.EN)

    fun compose(candidate: AlertCandidate, lang: AlertLang = AlertLang.current()): String =
        compose(candidate.detection, candidate.caution, lang)

    fun compose(
        detection: Detection,
        caution: Boolean = false,
        lang: AlertLang = AlertLang.current(),
    ): String {
        if (caution) return cautiousMessage(lang)
        val label = humanLabel(detection.label, lang)
        val proximity = proximityPhrase(detection.proximity, lang)
        val direction = directionPhrase(detection.sector, lang)
        val message = listOf(label, proximity, direction)
            .filter { it.isNotEmpty() }
            .joinToString(separator = " ")
            .let { ensurePeriod(capitalizeFirst(it)) }
        check(wordCount(message) <= MAX_ALERT_WORDS) {
            "Alert exceeds $MAX_ALERT_WORDS words: $message"
        }
        return message
    }

    private fun humanLabel(raw: String, lang: AlertLang): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return lang.genericObstacle
        return lang.label(trimmed)
    }

    private fun directionPhrase(sector: HorizontalSector, lang: AlertLang): String =
        when (lang) {
            AlertLang.ES -> when (sector) {
                HorizontalSector.LEFT -> "a la izquierda"
                HorizontalSector.RIGHT -> "a la derecha"
                HorizontalSector.CENTER -> "delante"
            }
            AlertLang.EN -> when (sector) {
                HorizontalSector.LEFT -> "on left"
                HorizontalSector.RIGHT -> "on right"
                HorizontalSector.CENTER -> "ahead"
            }
        }

    private fun proximityPhrase(proximity: Proximity, lang: AlertLang): String =
        when (lang) {
            AlertLang.ES -> when (proximity) {
                Proximity.NEAR -> "cerca"
                Proximity.MEDIUM -> "media"
                Proximity.FAR -> "lejos"
                Proximity.UNKNOWN -> ""
            }
            AlertLang.EN -> when (proximity) {
                Proximity.NEAR -> "near"
                Proximity.MEDIUM -> "medium"
                Proximity.FAR -> "far"
                Proximity.UNKNOWN -> ""
            }
        }

    private fun ensurePeriod(text: String): String =
        if (text.endsWith(".")) text else "$text."

    private fun capitalizeFirst(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
}

/** Alert message language; follows the device locale by default. */
enum class AlertLang(val genericObstacle: String) {
    ES("obstáculo") {
        private val labels = mapOf(
            "person" to "persona",
            "chair" to "silla",
            "table" to "mesa",
            "dining table" to "mesa",
            "door" to "puerta",
            "wall" to "pared",
            "barrier" to "barrera",
            "car" to "coche",
            "truck" to "camión",
            "bus" to "autobús",
            "bicycle" to "bicicleta",
            "motorcycle" to "motocicleta",
            "dog" to "perro",
            "cat" to "gato",
            "backpack" to "mochila",
            "suitcase" to "maleta",
            "umbrella" to "paraguas",
            "bench" to "banco",
            "potted plant" to "planta",
            "bed" to "cama",
            "couch" to "sofá",
            "tv" to "televisor",
            "laptop" to "portátil",
            "keyboard" to "teclado",
            "cell phone" to "teléfono",
            "book" to "libro",
            "cup" to "taza",
            "bottle" to "botella",
            "stop sign" to "señal de stop",
            "traffic light" to "semáforo",
            "generic obstacle" to "obstáculo",
            "obstacle" to "obstáculo",
        )

        override fun label(raw: String): String =
            labels[raw.lowercase()] ?: raw.lowercase()
    },
    EN("obstacle") {
        override fun label(raw: String): String = raw.lowercase()
    };

    abstract fun label(raw: String): String

    companion object {
        fun current(): AlertLang {
            val language = Locale.getDefault().language
            return if (language.equals("es", ignoreCase = true)) ES else EN
        }
    }
}
