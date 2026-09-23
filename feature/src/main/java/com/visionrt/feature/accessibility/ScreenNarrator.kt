package com.visionrt.feature.accessibility

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.visionrt.feature.R

/**
 * Builds a spoken description of a screen in visual top-to-bottom order
 * (UX-001 / FR-010): header text followed by every visible button,
 * numbered from the top of the screen.
 */
object ScreenNarrator {

    /** Returns [headers] plus a numbered list of visible buttons under [root]. */
    fun describe(context: Context, root: View, vararg headers: String): String {
        val buttons = mutableListOf<String>()
        collectVisibleButtons(root, buttons)
        return compose(
            headers = headers.filter { it.isNotBlank() },
            buttonLabels = buttons,
            fromTop = context.getString(R.string.narrator_from_top),
            buttonFmt = { index, label ->
                context.getString(R.string.narrator_button, index, label)
            },
        )
    }

    /**
     * Pure composition used by [describe] and unit tests.
     * Joins headers, then "from top:" and one numbered button per label.
     */
    fun compose(
        headers: List<String>,
        buttonLabels: List<String>,
        fromTop: String,
        buttonFmt: (Int, String) -> String,
    ): String {
        val sb = StringBuilder()
        headers.filter { it.isNotBlank() }.forEach { header ->
            if (sb.isNotEmpty()) sb.append(". ")
            sb.append(header.trim().removeSuffix("."))
        }
        if (buttonLabels.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(". ")
            sb.append(fromTop)
            buttonLabels.forEachIndexed { i, label ->
                sb.append(" ").append(buttonFmt(i + 1, label)).append(".")
            }
        } else if (sb.isNotEmpty()) {
            sb.append(".")
        }
        return sb.toString()
    }

    private fun collectVisibleButtons(view: View, out: MutableList<String>) {
        if (!isVisibleInHierarchy(view)) return
        if (view is Button) {
            val label = view.text?.toString()?.trim().orEmpty()
            if (label.isNotEmpty()) out += label
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectVisibleButtons(view.getChildAt(i), out)
            }
        }
    }

    private fun isVisibleInHierarchy(view: View): Boolean {
        var current: View? = view
        while (current != null) {
            if (current.visibility != View.VISIBLE) return false
            current = current.parent as? View
        }
        return true
    }
}
