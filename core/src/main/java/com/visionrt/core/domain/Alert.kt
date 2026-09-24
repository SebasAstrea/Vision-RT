package com.visionrt.core.domain

/**
 * One feedback unit. [sector] and [proximity] are present for obstacle
 * alerts so the dispatcher can pick earcons/haptic patterns (FR-010/FR-011);
 * status/OCR/debug alerts may leave them null.
 */
data class Alert(
    val id: String,
    val priority: AlertPriority,
    val message: String,
    val createdAtMs: Long,
    val sector: HorizontalSector? = null,
    val proximity: Proximity? = null,
)
