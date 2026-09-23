package com.visionrt.core.assistance

/**
 * Application-service port for starting/stopping obstacle assistance
 * (ARCHITECTURE §7.1: feature depends on core interfaces, not implementations).
 *
 * The app module binds a CameraX + LiteRT implementation; the UI only calls
 * [start]/[stop] after the camera permission has been granted. Lifecycle for
 * CameraX is owned inside the app (ProcessLifecycleOwner), so core stays free
 * of androidx.lifecycle types.
 */
interface AssistanceController {
    /** True while obstacle assistance is actively processing frames. */
    fun isActive(): Boolean

    /** Starts the pipeline; returns success/failure the UI can announce. */
    suspend fun start(): Result<Unit>

    /** Stops pipeline, releases camera and unloads the detector. */
    suspend fun stop(): Result<Unit>
}
