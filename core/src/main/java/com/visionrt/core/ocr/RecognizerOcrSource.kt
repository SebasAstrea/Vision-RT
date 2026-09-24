package com.visionrt.core.ocr

import com.visionrt.core.orchestration.OcrSource

/**
 * [OcrSource] adapter over [TextRecognizer] + [OcrLifecycle].
 *
 * Loads on demand (lazy), touches the idle timer on each recognize, and maps
 * empty/missing frames to failure so orchestration can announce FR-009.8.
 * Recognized text is returned only to the session feedback path — never logged.
 */
class RecognizerOcrSource(
    private val recognizer: TextRecognizer,
    private val lifecycle: OcrLifecycle,
    private var pendingCapture: () -> TextCapture?,
) : OcrSource {

    fun updateCaptureProvider(provider: () -> TextCapture?) {
        pendingCapture = provider
    }

    override suspend fun recognize(): Result<String> =
        recognizeBlocks().map { recognized ->
            if (recognized.isEmpty) "" else recognized.blocks.joinToString("\n") { it.text }
        }

    /** Full result with block structure for Text Reading Mode. */
    suspend fun recognizeBlocks(): Result<RecognizedText> =
        lifecycle.ensureLoaded().fold(
            onSuccess = {
                lifecycle.touch()
                val capture = pendingCapture()
                if (capture == null) {
                    Result.failure(IllegalStateException("No frame available for OCR"))
                } else {
                    recognizer.recognize(capture).also { lifecycle.touch() }
                }
            },
            onFailure = { Result.failure(it) },
        )
}
