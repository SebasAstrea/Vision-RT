package com.visionrt.core.ocr

/**
 * Block-level reading controller for FR-009.4 (repeat / next / stop).
 * Pure state machine — UI and TTS stay outside (feature module).
 */
class TextReadingSession {
    var blocks: List<TextBlock> = emptyList()
        private set

    /** Index of the block the user is on, or -1 when nothing loaded. */
    var currentIndex: Int = -1
        private set

    var isReading: Boolean = false
        private set

    fun load(newBlocks: List<TextBlock>): Boolean {
        val ordered = newBlocks
            .filter { it.text.isNotBlank() }
            .sortedBy { it.readingOrder }
        blocks = ordered
        currentIndex = if (ordered.isEmpty()) -1 else 0
        isReading = ordered.isNotEmpty()
        return isReading
    }

    fun currentBlock(): TextBlock? = blocks.getOrNull(currentIndex)

    /** Repeat the current block. Returns text to speak, or null if empty. */
    fun repeat(): String? {
        if (!isReading) return null
        return currentBlock()?.text
    }

    /** Advance to the next block. Returns its text, or null at the end. */
    fun next(): String? {
        val canAdvance = isReading && blocks.isNotEmpty() && currentIndex + 1 < blocks.size
        return if (canAdvance) {
            currentIndex += 1
            currentBlock()?.text
        } else {
            null
        }
    }

    fun hasMore(): Boolean = isReading && currentIndex + 1 < blocks.size

    /** Stop reading and release the session's navigation state. */
    fun stop() {
        isReading = false
        currentIndex = -1
    }

    companion object {
        /** FR-009 failure copy when quality is insufficient. */
        fun unclearMessage(lang: com.visionrt.core.orchestration.AlertLang): String =
            when (lang) {
                com.visionrt.core.orchestration.AlertLang.ES ->
                    "El texto no es claro. Acércate o mejora la luz."
                com.visionrt.core.orchestration.AlertLang.EN ->
                    "Text is not clear. Try closer or better light."
            }

        fun emptyMessage(lang: com.visionrt.core.orchestration.AlertLang): String =
            when (lang) {
                com.visionrt.core.orchestration.AlertLang.ES -> "No se encontró texto."
                com.visionrt.core.orchestration.AlertLang.EN -> "No text found."
            }

        fun endMessage(lang: com.visionrt.core.orchestration.AlertLang): String =
            when (lang) {
                com.visionrt.core.orchestration.AlertLang.ES -> "Fin del texto."
                com.visionrt.core.orchestration.AlertLang.EN -> "End of text."
            }
    }
}
