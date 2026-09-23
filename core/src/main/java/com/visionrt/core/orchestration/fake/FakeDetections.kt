package com.visionrt.core.orchestration.fake

import com.visionrt.core.domain.BoundingBox
import com.visionrt.core.domain.Detection
import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity

/**
 * Shared helpers to build [Detection] values in orchestration tests without
 * depending on perception or inference modules.
 */
object FakeDetections {

    private const val BOX_LEFT = 0.1f
    private const val BOX_TOP = 0.1f
    private const val BOX_RIGHT = 0.4f
    private const val BOX_BOTTOM = 0.6f

    fun of(
        label: String,
        confidence: Float = 0.90f,
        sector: HorizontalSector = HorizontalSector.CENTER,
        proximity: Proximity = Proximity.MEDIUM,
        timestampMs: Long = 0L,
    ): Detection = Detection(
        id = "d-$label-$sector-$timestampMs",
        classId = 0,
        label = label,
        confidence = confidence,
        boundingBox = BoundingBox(BOX_LEFT, BOX_TOP, BOX_RIGHT, BOX_BOTTOM),
        sector = sector,
        proximity = proximity,
        timestampMs = timestampMs,
        sourceModelVersion = "fake-1",
    )
}
