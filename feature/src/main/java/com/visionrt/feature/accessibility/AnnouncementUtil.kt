package com.visionrt.feature.accessibility

import android.view.View

/**
 * Accessibility announcement helpers (ARCHITECTURE.md §13.3). Live
 * announcements are anchored to a live View so TalkBack delivers them in the
 * correct context. Usage examples: assistance started/stopped, degraded mode,
 * settings changes.
 */
object AnnouncementUtil {

    /** Announces [text] through the platform screen reader, anchored to [anchor]. */
    fun announce(anchor: View, text: String) {
        anchor.announceForAccessibility(text)
    }

    /** Announces on a container without stealing focus from its current target. */
    fun announcePolite(anchor: View, viewToAnnounce: View, text: String) {
        if (!anchor.isLaidOut) return
        viewToAnnounce.announceForAccessibility(text)
    }
}
