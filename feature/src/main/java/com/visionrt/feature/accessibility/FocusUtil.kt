package com.visionrt.feature.accessibility

import android.view.View

/**
 * Explicit focus management (ARCHITECTURE.md §13.2): assistance start/stop,
 * mode changes, dialogs and errors must move focus predictably and never trap
 * it without an accessible exit.
 */
object FocusUtil {

    /** Moves input/a11y focus to [target] and optionally announces the change. */
    fun moveAccessibilityFocusTo(target: View, announce: String? = null) {
        if (!target.isShown) return
        target.requestFocus()
        if (announce != null) {
            target.announceForAccessibility(announce)
        }
    }
}
