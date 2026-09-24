package com.visionrt.core.ocr.fake

import com.visionrt.core.ocr.LanguagePack
import com.visionrt.core.ocr.RecognizedText
import com.visionrt.core.ocr.TextBlock
import com.visionrt.core.ocr.TextCapture
import com.visionrt.core.ocr.TextRecognizer

/**
 * Deterministic [TextRecognizer] double for unit tests and instrumented
 * wiring when ML Kit is unavailable. Stage results with [stageBlocks] or
 * [failNext]; never logs recognized text.
 */
class FakeTextRecognizer : TextRecognizer {

    private var pendingBlocks: List<TextBlock>? = null
    private var failNext: Boolean = false
    private var loaded: Boolean = false

    var loadCallCount: Int = 0
        private set
    var unloadCallCount: Int = 0
        private set
    var recognizeCallCount: Int = 0
        private set

    var lastLanguagePack: LanguagePack? = null
        private set

    fun stageBlocks(blocks: List<TextBlock>) {
        pendingBlocks = blocks
    }

    fun stagePlain(text: String) {
        pendingBlocks = listOf(TextBlock(text = text, readingOrder = 0))
    }

    fun failNext() {
        failNext = true
    }

    override suspend fun load(languagePack: LanguagePack): Result<Unit> {
        loadCallCount++
        lastLanguagePack = languagePack
        loaded = true
        return Result.success(Unit)
    }

    override suspend fun recognize(image: TextCapture): Result<RecognizedText> {
        recognizeCallCount++
        val failure = when {
            failNext -> {
                failNext = false
                IllegalStateException("fake recognizer failure")
            }
            !loaded -> IllegalStateException("fake recognizer not loaded")
            else -> null
        }
        if (failure != null) return Result.failure(failure)
        val blocks = pendingBlocks.orEmpty()
        pendingBlocks = null
        return Result.success(RecognizedText(blocks = blocks, languageTag = "und"))
    }

    override fun isLoaded(): Boolean = loaded

    override suspend fun unload() {
        unloadCallCount++
        loaded = false
    }
}
