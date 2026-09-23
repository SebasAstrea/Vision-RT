package com.visionrt.feature.accessibility

import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.visionrt.feature.R
import com.visionrt.feature.voice.ScreenReaderGate
import com.visionrt.feature.voice.ScreenVoice
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Two-step activation for every interactive control (UX-001):
 * the first tap only announces which button was touched; the second tap on
 * the same button runs the action. Skipped when a screen reader is active
 * because TalkBack already double-taps to activate and speaks focus labels.
 */
class ConfirmTaps @Inject constructor(
    private val voice: ScreenVoice,
    private val screenReader: ScreenReaderGate,
) {

    private var pendingId: Int = View.NO_ID
    private var timeoutJob: Job? = null

    fun attach(view: View, scope: LifecycleCoroutineScope, action: (View) -> Unit) {
        if (screenReader.isScreenReaderActive()) {
            view.setOnClickListener { action(it) }
            return
        }
        view.setOnClickListener { v ->
            if (pendingId == v.id) {
                disarm()
                action(v)
            } else {
                arm(v, scope)
            }
        }
    }

    private fun arm(view: View, scope: LifecycleCoroutineScope) {
        pendingId = view.id
        timeoutJob?.cancel()
        val label = buttonLabel(view)
        val message = view.context.getString(R.string.confirm_tap, label)
        AnnouncementUtil.announce(view, message)
        scope.launch { voice.speakNow(message) }
        timeoutJob = scope.launch {
            delay(CONFIRM_TIMEOUT_MS)
            if (pendingId == view.id) disarm()
        }
    }

    private fun disarm() {
        pendingId = View.NO_ID
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun buttonLabel(view: View): String = when (view) {
        is TextView -> view.text?.toString()?.trim().orEmpty()
        else -> view.contentDescription?.toString()?.trim().orEmpty()
    }

    private companion object {
        const val CONFIRM_TIMEOUT_MS = 5_000L
    }
}
