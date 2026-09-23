package com.visionrt.feature.voice

import android.content.Context
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [ScreenReaderGate] backed by [AccessibilityManager]: active when a screen
 * reader is enabled and touch exploration (TalkBack mode) is on.
 */
class AccessibilityScreenReaderGate @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScreenReaderGate {

    override fun isScreenReaderActive(): Boolean {
        val am = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return am.isEnabled && am.isTouchExplorationEnabled
    }
}
