package com.visionrt.core.summary

/**
 * Application-service port for FR-008 on-demand object summary
 * (ARCHITECTURE §7.1: feature calls core, app wires CameraX + LiteRT).
 *
 * One-shot describe of the current view using detector classes + direction +
 * proximity + templates — no VLM required (FR-008.3).
 */
interface ObjectSummaryService {
    /**
     * Captures/uses the latest frame, runs detection if needed, and returns
     * a concise spoken summary. Fails fast past FR-008.5 (8 s) so the UI can
     * announce the timeout message.
     */
    suspend fun describeScene(): Result<String>
}
