package com.visionrt.core.common

/**
 * Pure-Kotlin safe logging core (no Android APIs, unit-testable on the JVM).
 *
 * Guardrails (OBJECTIVE-02 / OR-002 requirement, M0 deliverable):
 * 1. Messages and metrics only; no raw payload types accepted by the public API.
 * 2. Any payload MUST be wrapped in a [SafePayload] *by design*.
 * 3. [rejectUnsafe] exists so callers who try to log raw camera frames, OCR
 *    text or PII fail loudly instead of silently leaking.
 */
object SafeLog {

    const val LOG_TAG_PREFIX = "VisionRT"

    @JvmStatic
    fun format(tag: String, message: String): String {
        val prefixed = if (tag.startsWith(LOG_TAG_PREFIX)) tag else "$LOG_TAG_PREFIX/$tag"
        return "$prefixed: $message"
    }

    @JvmStatic
    fun metric(tag: String, key: String, value: Double, unit: String = ""): String =
        format(tag, "$key=$value$unit")

    @JvmStatic
    fun safe(tag: String, message: String, payload: SafePayload): String =
        format(tag, "$message | ${payload.render()}")

    /** Fails loudly: raw media/PII must never reach the system log. */
    @JvmStatic
    fun rejectUnsafe(): Nothing =
        throw UnsafePayloadException(
            "SafeLog forbids raw camera frames, OCR text, audio samples and PII in logs"
        )
}

class UnsafePayloadException(message: String) : IllegalArgumentException(message)

data class SafePayload(
    val kind: SafePayloadKind,
    val text: String? = null,
    val numeric: Map<String, Number> = emptyMap(),
) {
    init {
        require(text != null || numeric.isNotEmpty()) { "SafePayload must carry allowed content" }
    }

    fun render(): String = when (kind) {
        SafePayloadKind.NUMERIC -> numeric.entries.joinToString(",") { "${it.key}=${it.value}" }
        SafePayloadKind.STATUS -> text.orEmpty()
    }

    enum class SafePayloadKind { NUMERIC, STATUS }
}
