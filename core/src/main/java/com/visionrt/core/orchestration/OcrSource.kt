package com.visionrt.core.orchestration

/**
 * Inbound OCR port consumed by orchestration. Recognized text must never be
 * logged (SafeLog); only the session feedback path may speak it.
 */
interface OcrSource {
    suspend fun recognize(): Result<String>
}
