package com.visionrt.core.orchestration

import com.visionrt.core.domain.Alert
import com.visionrt.core.domain.AlertPriority

/**
 * Speaks accepted mode transitions as STATUS alerts so no UI-visible state
 * change is silent (M2 exit criteria, FR-015). Language follows [AlertLang]
 * so status speech matches alert language.
 */
internal class StateAnnouncer(
    private val feedback: FeedbackPort,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val lang: AlertLang = AlertLang.current(),
) {
    suspend fun announce(from: OrchestrationState, to: OrchestrationState) {
        val message = announcement(from, to) ?: return
        feedback.emit(
            Alert(
                id = "state-$from-$to-${clock()}",
                priority = AlertPriority.STATUS,
                message = message,
                createdAtMs = clock(),
            ),
        )
    }

    private fun announcement(from: OrchestrationState, to: OrchestrationState): String? =
        when (lang) {
            AlertLang.ES -> announcementEs(from, to)
            AlertLang.EN -> announcementEn(from, to)
        }

    private fun announcementEs(from: OrchestrationState, to: OrchestrationState): String? =
        when (to) {
            OrchestrationState.STARTING -> "Iniciando asistencia."
            OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE ->
                if (from == OrchestrationState.STARTING) {
                    "Asistencia de obstáculos lista."
                } else {
                    "Asistencia de obstáculos activa."
                }
            OrchestrationState.OBJECT_QUERY_ACTIVE -> "Resumen de objetos listo."
            OrchestrationState.TEXT_READING_ACTIVE -> "Lectura de texto lista."
            OrchestrationState.DEGRADED -> "Asistencia limitada."
            OrchestrationState.ERROR -> "Asistencia detenida por un error."
            OrchestrationState.STOPPING -> "Deteniendo asistencia."
            OrchestrationState.IDLE ->
                if (from == OrchestrationState.STOPPING) "Asistencia detenida." else null
        }

    private fun announcementEn(from: OrchestrationState, to: OrchestrationState): String? =
        when (to) {
            OrchestrationState.STARTING -> "Starting assistance."
            OrchestrationState.OBSTACLE_ASSISTANCE_ACTIVE ->
                if (from == OrchestrationState.STARTING) {
                    "Obstacle assistance ready."
                } else {
                    "Obstacle assistance active."
                }
            OrchestrationState.OBJECT_QUERY_ACTIVE -> "Object query ready."
            OrchestrationState.TEXT_READING_ACTIVE -> "Text reading ready."
            OrchestrationState.DEGRADED -> "Assistance is limited."
            OrchestrationState.ERROR -> "Assistance stopped due to an error."
            OrchestrationState.STOPPING -> "Stopping assistance."
            OrchestrationState.IDLE ->
                if (from == OrchestrationState.STOPPING) "Assistance stopped." else null
        }
}
