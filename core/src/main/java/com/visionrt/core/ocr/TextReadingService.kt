package com.visionrt.core.ocr

/**
 * Application-service port for FR-009 Text Reading Mode
 * (ARCHITECTURE §7.1: feature → core only).
 *
 * Implementations own camera capture, ML Kit recognize, block navigation,
 * and the 30 s idle unload (OR-002.7). Recognized text is spoken only via
 * the feedback path — never logged.
 */
interface TextReadingService {
    /** Enters text-reading session, captures, OCRs, returns first block text. */
    suspend fun startReading(): Result<String>

    /** Repeats the current block. [Result] failure when nothing loaded. */
    suspend fun repeat(): Result<String>

    /**
     * Advances to the next block. Success with [Result] null-equivalent empty
     * is expressed as failure with [EndOfText] so the UI can announce end.
     */
    suspend fun next(): Result<String>

    /** Stops speaking/navigation and ends the session (does not unload OCR yet). */
    suspend fun stopReading()

    fun isActive(): Boolean

    /** Number of blocks loaded for the current capture (0 if none). */
    fun blockCount(): Int

    companion object {
        const val END_OF_TEXT = "END_OF_TEXT"
    }
}
