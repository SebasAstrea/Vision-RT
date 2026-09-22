package com.visionrt.core.common

import android.util.Log

/**
 * Thin Android wrapper around [SafeLog]. The only way the app touches logcat.
 *
 * No function here accepts raw images, OCR text, audio or PII. If a caller ever
 * needs to log a payload it must build a [SafePayload] explicitly, which makes
 * the safety review a compiler+code-review check, not a runtime hope.
 */
object SafeLogger {

    fun d(tag: String, message: String) =
        Log.d(SafeLog.LOG_TAG_PREFIX, SafeLog.format(tag, message))

    fun i(tag: String, message: String) =
        Log.i(SafeLog.LOG_TAG_PREFIX, SafeLog.format(tag, message))

    fun w(tag: String, message: String, t: Throwable? = null) =
        Log.w(SafeLog.LOG_TAG_PREFIX, SafeLog.format(tag, message), t)

    fun e(tag: String, message: String, t: Throwable? = null) =
        Log.e(SafeLog.LOG_TAG_PREFIX, SafeLog.format(tag, message), t)

    fun metric(tag: String, key: String, value: Double, unit: String = "") =
        Log.d(SafeLog.LOG_TAG_PREFIX, SafeLog.metric(tag, key, value, unit))

    fun safe(tag: String, message: String, payload: SafePayload) =
        Log.d(SafeLog.LOG_TAG_PREFIX, SafeLog.safe(tag, message, payload))

    @Deprecated(
        message = "Raw media/PII must never reach logcat",
        level = DeprecationLevel.ERROR
    )
    @Suppress("UnusedParameter")
    fun logRaw(payload: Any?): Nothing = SafeLog.rejectUnsafe()
}
