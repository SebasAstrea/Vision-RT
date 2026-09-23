package com.visionrt.core.orchestration.fake

import com.visionrt.core.domain.Detection
import com.visionrt.core.orchestration.DetectionSource

/**
 * Deterministic detector double (M2 deliverable). [push] stages the next
 * frame's detections (latest-only: a new push before consume replaces the
 * pending frame). Each [detect] consumes the staged frame once; subsequent
 * calls without a push re-serve the last consumed frame (sticky scene).
 * [failNextDetect] forces a failed Result once.
 */
class FakeDetectionSource : DetectionSource {

    private var pending: List<Detection>? = null
    private var lastConsumed: List<Detection> = emptyList()
    private var failNext: Boolean = false

    var detectCallCount: Int = 0
        private set

    fun push(detections: List<Detection>) {
        pending = detections
    }

    fun failNextDetect() {
        failNext = true
    }

    override suspend fun detect(): Result<List<Detection>> {
        detectCallCount++
        if (failNext) {
            failNext = false
            return Result.failure(IllegalStateException("fake detector failure"))
        }
        val staged = pending
        if (staged != null) {
            lastConsumed = staged
            pending = null
        }
        return Result.success(lastConsumed)
    }
}
