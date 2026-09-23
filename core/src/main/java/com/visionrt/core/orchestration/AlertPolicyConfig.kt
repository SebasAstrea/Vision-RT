package com.visionrt.core.orchestration

/**
 * Tunable alert thresholds (ARCHITECTURE §9.5, OR-008). Defaults are QA-visible,
 * not hard-coded at call sites.
 */
data class AlertPolicyConfig(
    val confidenceThreshold: Float = 0.60f,
    val highConfidenceThreshold: Float = 0.80f,
    val minStableFrames: Int = 2,
    val cooldownMs: Long = 5_000L,
    /** Below this confidence (but >= [confidenceThreshold]) use cautious wording. */
    val cautionConfidenceThreshold: Float = 0.70f,
) {
    init {
        require(confidenceThreshold in 0f..1f)
        require(highConfidenceThreshold in 0f..1f)
        require(cautionConfidenceThreshold >= confidenceThreshold)
        require(highConfidenceThreshold >= cautionConfidenceThreshold)
        require(minStableFrames >= 1)
        require(cooldownMs >= 0L)
    }
}
