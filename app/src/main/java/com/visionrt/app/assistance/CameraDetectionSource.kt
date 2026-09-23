package com.visionrt.app.assistance

import com.visionrt.core.domain.Detection
import com.visionrt.core.orchestration.DetectionSource
import com.visionrt.inference.runtimeapi.ObjectDetector
import com.visionrt.perception.camera.CameraFrameSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges [CameraFrameSource] latest frame → [ObjectDetector] into the core
 * [DetectionSource] port consumed by orchestration (M3 deliverable: real
 * detections reach AlertPolicy). Empty gate or unloaded detector → empty list.
 */
@Singleton
class CameraDetectionSource @Inject constructor(
    private val camera: CameraFrameSource,
    private val detector: ObjectDetector,
) : DetectionSource {

    override suspend fun detect(): Result<List<Detection>> {
        val frame = camera.consumeLatestFrame()
        return if (frame == null || !detector.isLoaded()) {
            Result.success(emptyList())
        } else {
            detector.detect(frame)
        }
    }
}
