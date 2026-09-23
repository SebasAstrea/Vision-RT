package com.visionrt.core.orchestration

import com.visionrt.core.domain.Detection

/**
 * Inbound detector port consumed by orchestration. The real LiteRT adapter
 * (inference module) and the M2 fake both implement this; orchestration does
 * not depend on a specific runtime.
 */
interface DetectionSource {
    /** Returns detections for the latest processed frame (never null; empty = none). */
    suspend fun detect(): Result<List<Detection>>
}
