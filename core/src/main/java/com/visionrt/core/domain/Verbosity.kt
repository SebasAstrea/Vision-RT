package com.visionrt.core.domain

/**
 * Spoken alert verbosity (FR-006.5). Stored in user preferences (M1) and
 * consumed by the alert composer (M2+).
 */
enum class Verbosity(val key: String) {
    MINIMAL("minimal"),
    NORMAL("normal"),
    DETAILED("detailed");

    companion object {
        fun fromKey(key: String): Verbosity = entries.firstOrNull { it.key == key } ?: NORMAL
    }
}
