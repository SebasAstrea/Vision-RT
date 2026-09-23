package com.visionrt.core.orchestration

/**
 * Latest-only holder for a single in-flight value (ARCHITECTURE §10.2).
 * Submitting while a value is unread replaces it; consumers always see the
 * newest item and never queue backlog.
 */
class LatestOnlyChannel<T> {
    private var value: T? = null
    private var hasValue: Boolean = false

    @Synchronized
    fun submit(item: T) {
        value = item
        hasValue = true
    }

    @Synchronized
    fun tryConsume(): T? {
        if (!hasValue) return null
        hasValue = false
        val result = value
        value = null
        return result
    }

    @Synchronized
    fun peek(): T? = value

    @Synchronized
    fun clear() {
        value = null
        hasValue = false
    }
}
