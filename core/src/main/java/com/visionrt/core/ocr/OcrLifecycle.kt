package com.visionrt.core.ocr

/**
 * Lifecycle port for the OCR engine (OR-002.7): lazy-load on demand, unload
 * after [IDLE_UNLOAD_MS] of inactivity so detector + OCR never both stay
 * resident (OR-002.6).
 *
 * The app module owns the timer; core only declares the contract and budget.
 */
interface OcrLifecycle {
    /** Ensure the recognizer is loaded; no-op when already loaded. */
    suspend fun ensureLoaded(): Result<Unit>

    /** Mark activity so the idle timer restarts. */
    fun touch()

    /** Unload if loaded. Safe to call when not loaded. */
    suspend fun unload()

    fun isLoaded(): Boolean

    companion object {
        /** OR-002.7: unload OCR after 30 seconds without use. */
        const val IDLE_UNLOAD_MS: Long = 30_000L
    }
}
