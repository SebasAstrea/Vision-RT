package com.visionrt.core.orchestration

import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.domain.Detection
import com.visionrt.core.domain.Proximity
import com.visionrt.core.domain.Verbosity

/**
 * A detection that passed confidence, temporal persistence and cooldown checks
 * (FR-005.6, FR-006, OR-008). Not yet a spoken [com.visionrt.core.domain.Alert].
 */
data class AlertCandidate(
    val detection: Detection,
    val priority: AlertPriority,
    /** True when confidence is stable but low enough for cautious wording. */
    val caution: Boolean,
)

/**
 * Converts per-frame detections into stable alert candidates.
 *
 * Rules (OR-008):
 * 1. Confidence must reach [AlertPolicyConfig.confidenceThreshold].
 * 2. A critical alert needs [AlertPolicyConfig.minStableFrames] consecutive
 *    frames, or a single high-confidence frame plus motion evidence, or a
 *    single high-confidence NEAR detection (object already in the path —
 *    waiting another frame at 8 FPS delays the alert until it is too close).
 * 3. The same object key is suppressed for the verbosity cooldown unless
 *    proximity risk increases (FR-006.4).
 */
class AlertPolicy(
    private val config: AlertPolicyConfig = AlertPolicyConfig(),
) {
    private data class Track(
        var consecutiveFrames: Int = 0,
        var lastAlertAtMs: Long = Long.MIN_VALUE,
        var lastAlertProximity: Proximity = Proximity.UNKNOWN,
    )

    private val tracks = mutableMapOf<String, Track>()
    private var previousFrameKeys: Set<String> = emptySet()

    fun evaluate(
        detections: List<Detection>,
        nowMs: Long,
        motionEvidence: Boolean = false,
        verbosity: Verbosity = Verbosity.NORMAL,
    ): List<AlertCandidate> {
        val cooldownMs = config.cooldownFor(detailed = verbosity == Verbosity.DETAILED)
        val frameKeys = mutableSetOf<String>()
        val candidates = mutableListOf<AlertCandidate>()
        detections.forEach { detection ->
            if (detection.confidence < config.confidenceThreshold) return@forEach
            val key = fusionKey(detection)
            frameKeys += key
            val track = tracks.getOrPut(key) { Track() }
            track.consecutiveFrames =
                if (key in previousFrameKeys) track.consecutiveFrames + 1 else 1
            if (isStable(track, detection, motionEvidence) &&
                cooldownAllows(track, detection, nowMs, cooldownMs)
            ) {
                track.lastAlertAtMs = nowMs
                track.lastAlertProximity = detection.proximity
                candidates += AlertCandidate(
                    detection = detection,
                    priority = AlertPriority.CRITICAL_OBSTACLE,
                    caution = detection.confidence < config.cautionConfidenceThreshold,
                )
            }
        }
        previousFrameKeys = frameKeys
        tracks.keys.retainAll(frameKeys)
        return candidates
    }

    fun reset() {
        tracks.clear()
        previousFrameKeys = emptySet()
    }

    private fun fusionKey(detection: Detection): String =
        detection.label.lowercase()

    private fun isStable(track: Track, detection: Detection, motionEvidence: Boolean): Boolean {
        // Near + high confidence: alert on the first frame so the user is not
        // warned only after the object is already "on top of" the camera.
        if (detection.proximity == Proximity.NEAR &&
            detection.confidence >= config.confidenceThreshold
        ) {
            return true
        }
        return track.consecutiveFrames >= config.minStableFrames ||
            (
                track.consecutiveFrames >= 1 &&
                    detection.confidence >= config.highConfidenceThreshold &&
                    motionEvidence
                )
    }

    private fun cooldownAllows(
        track: Track,
        detection: Detection,
        nowMs: Long,
        cooldownMs: Long,
    ): Boolean {
        if (track.lastAlertAtMs == Long.MIN_VALUE) return true
        val withinCooldown = nowMs - track.lastAlertAtMs < cooldownMs
        return !withinCooldown || proximityRiskIncreased(track.lastAlertProximity, detection.proximity)
    }

    private fun proximityRiskIncreased(previous: Proximity, current: Proximity): Boolean =
        riskRank(current) > riskRank(previous)

    private fun riskRank(proximity: Proximity): Int = when (proximity) {
        Proximity.NEAR -> RISK_NEAR
        Proximity.MEDIUM -> RISK_MEDIUM
        Proximity.FAR -> RISK_FAR
        Proximity.UNKNOWN -> RISK_UNKNOWN
    }

    private companion object {
        const val RISK_UNKNOWN = 0
        const val RISK_FAR = 1
        const val RISK_MEDIUM = 2
        const val RISK_NEAR = 3
    }
}
