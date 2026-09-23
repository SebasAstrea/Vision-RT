package com.visionrt.feature.voice

/** True when a platform screen reader (e.g. TalkBack) is active. */
fun interface ScreenReaderGate {
    fun isScreenReaderActive(): Boolean
}
