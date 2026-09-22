package com.visionrt.perception

/**
 * Immutable, allocator-light snapshot of one camera frame (no Bitmap bytes —
 * the image buffer lives in [framegate.FrameGate], never in logs).
 */
data class PerceptionFrame(
    val timestampMs: Long,
    val width: Int,
    val height: Int,
)
