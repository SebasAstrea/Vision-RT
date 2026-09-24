package com.visionrt.core.resource

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority
import com.visionrt.core.orchestration.AlertLang
import com.visionrt.core.orchestration.FeedbackPort

/**
 * Accessible degradation notifications (FR-015.4–6, OR-006, OR-010.5, M6
 * deliverable 7). Emits STATUS alerts when the ladder level or cause changes
 * so TalkBack/TTS users hear heat, battery-saver and limited-assistance copy.
 */
class DegradationAnnouncer(
    private val feedback: FeedbackPort,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val lang: AlertLang = AlertLang.current(),
) {
    private var lastLevel: DegradationLevel = DegradationLevel.NORMAL
    private var lastCause: ResourceCause = ResourceCause.NONE

    /**
 * Announces if [level]/[cause] differ from the previous emission.
 * Returns true when an alert was emitted (diagnostics can log it too).
 */
@Suppress("ReturnCount") // early exits for no-op / no-message paths
suspend fun onGovernorState(
    level: DegradationLevel,
    cause: ResourceCause,
): Boolean {
    if (level == lastLevel && cause == lastCause) return false
    val message = messageFor(level, cause) ?: run {
        lastLevel = level
        lastCause = cause
        return false
    }
    lastLevel = level
    lastCause = cause
    feedback.emit(
        Alert(
            id = "degradation-$level-$cause-${clock()}",
            priority = AlertPriority.STATUS,
            message = message,
            createdAtMs = clock(),
        ),
    )
    return true
}

    fun reset() {
        lastLevel = DegradationLevel.NORMAL
        lastCause = ResourceCause.NONE
    }

    private fun messageFor(level: DegradationLevel, cause: ResourceCause): String? =
        when (lang) {
            AlertLang.ES -> messageEs(level, cause)
            AlertLang.EN -> messageEn(level, cause)
        }

    @Suppress("CyclomaticComplexMethod") // policy table for accessible copy
    private fun messageEs(level: DegradationLevel, cause: ResourceCause): String? = when {
        level == DegradationLevel.NORMAL && cause == ResourceCause.RECOVERED ->
            "Rendimiento normal restablecido."
        cause == ResourceCause.THERMAL && level.isAtLeast(DegradationLevel.CRITICAL) ->
            "Asistencia limitada por el calor del dispositivo."
        cause == ResourceCause.THERMAL ->
            "Modo reducido por el calor del dispositivo."
        cause == ResourceCause.BATTERY_SAVER ->
            "Ahorro de batería activo. Modo reducido."
        cause == ResourceCause.BATTERY_LOW && level.isAtLeast(DegradationLevel.MINIMAL) ->
            "Batería muy baja. La asistencia puede detenerse."
        cause == ResourceCause.BATTERY_LOW ->
            "Batería baja. Modo reducido."
        cause == ResourceCause.MEMORY && level.isAtLeast(DegradationLevel.MINIMAL) ->
            "Memoria crítica. Asistencia limitada."
        cause == ResourceCause.MEMORY ->
            "Memoria alta. Modo reducido."
        cause == ResourceCause.LATENCY ->
            "Rendimiento reducido por lentitud."
        cause == ResourceCause.CAMERA_LOST ->
            "Cámara no disponible. Asistencia limitada."
        level.isAtLeast(DegradationLevel.REDUCED) -> "Asistencia limitada."
        else -> null
    }

    @Suppress("CyclomaticComplexMethod") // policy table for accessible copy
    private fun messageEn(level: DegradationLevel, cause: ResourceCause): String? = when {
        level == DegradationLevel.NORMAL && cause == ResourceCause.RECOVERED ->
            "Normal performance restored."
        cause == ResourceCause.THERMAL && level.isAtLeast(DegradationLevel.CRITICAL) ->
            "Assistance limited due to device heat."
        cause == ResourceCause.THERMAL ->
            "Reduced mode due to device heat."
        cause == ResourceCause.BATTERY_SAVER ->
            "Battery saver active."
        cause == ResourceCause.BATTERY_LOW && level.isAtLeast(DegradationLevel.MINIMAL) ->
            "Battery very low. Assistance may stop."
        cause == ResourceCause.BATTERY_LOW ->
            "Battery low. Reduced mode."
        cause == ResourceCause.MEMORY && level.isAtLeast(DegradationLevel.MINIMAL) ->
            "Memory critical. Assistance limited."
        cause == ResourceCause.MEMORY ->
            "High memory. Reduced mode."
        cause == ResourceCause.LATENCY ->
            "Reduced mode due to slow performance."
        cause == ResourceCause.CAMERA_LOST ->
            "Camera unavailable. Assistance limited."
        level.isAtLeast(DegradationLevel.REDUCED) -> "Assistance limited."
        else -> null
    }
}
