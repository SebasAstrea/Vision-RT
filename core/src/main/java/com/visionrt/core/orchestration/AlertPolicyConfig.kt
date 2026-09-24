package com.visionrt.core.orchestration

/**
 * Tunable alert thresholds (ARCHITECTURE §9.5, OR-008). Defaults are QA-visible,
 * not hard-coded at call sites.
 */
data class AlertPolicyConfig(
    val confidenceThreshold: Float = 0.60f,
    val highConfidenceThreshold: Float = 0.80f,
    val minStableFrames: Int = 2,
    val cooldownMs: Long = 4_000L,
    /** Below this confidence (but >= [confidenceThreshold]) use cautious wording. */
    val cautionConfidenceThreshold: Float = 0.70f,
    /**
     * DETAILED verbosity shortens cooldown so the user is not left in silence
     * after a couple of phrases (FR-006.5 / UX-004). User-tuned to 1.5s on
     * SM-A226BR after field feedback (2s still felt laggy).
     */
    val detailedCooldownMs: Long = 2_000L,
) {
    init {
        require(confidenceThreshold in 0f..1f)
        require(highConfidenceThreshold in 0f..1f)
        require(cautionConfidenceThreshold >= confidenceThreshold)
        require(highConfidenceThreshold >= cautionConfidenceThreshold)
        require(minStableFrames >= 1)
        require(cooldownMs >= 0L)
        require(detailedCooldownMs in 0L..cooldownMs)
    }

    fun cooldownFor(detailed: Boolean): Long =
        if (detailed) detailedCooldownMs else cooldownMs
}
