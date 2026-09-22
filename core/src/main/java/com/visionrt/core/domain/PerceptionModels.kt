package com.visionrt.core.domain

enum class HorizontalSector { LEFT, CENTER, RIGHT }

enum class Proximity { NEAR, MEDIUM, FAR, UNKNOWN }

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(right > left && bottom > top) { "Invalid bbox: inverted axes" }
    }
}

data class Detection(
    val id: String,
    val classId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val sector: HorizontalSector,
    val proximity: Proximity,
    val timestampMs: Long,
    val sourceModelVersion: String,
)
