package com.visionrt.core.orchestration.fake

import com.visionrt.core.orchestration.OcrSource

/**
 * Deterministic OCR double (M2 deliverable). Stage the next result with
 * [stageText] or [failNext] once to force a failure. Text is never logged here.
 */
class FakeOcrSource : OcrSource {

    private var pending: String? = null
    private var lastConsumed: String = ""
    private var failNext: Boolean = false

    var recognizeCallCount: Int = 0
        private set

    fun stageText(value: String) {
        pending = value
    }

    fun failNext() {
        failNext = true
    }

    override suspend fun recognize(): Result<String> {
        recognizeCallCount++
        if (failNext) {
            failNext = false
            return Result.failure(IllegalStateException("fake ocr failure"))
        }
        val staged = pending
        if (staged != null) {
            lastConsumed = staged
            pending = null
        }
        return Result.success(lastConsumed)
    }
}
