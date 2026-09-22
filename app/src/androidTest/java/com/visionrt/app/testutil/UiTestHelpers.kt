package com.visionrt.app.testutil

import android.view.View
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertFalse

fun resetSettingsState() {
    TestSettingsRepository.STATE.reset()
}

/** Wires the Espresso accessibility scanner into every screen interaction
 * (UX-002.5: automated scans must report zero critical defects). */
fun enableAccessibilityChecks() {
    AccessibilityChecks.enable()
}

fun click(matcher: Matcher<View>) {
    Espresso.onView(matcher).perform(ViewActions.click())
}

/** Clicks a view inside a ScrollView, scrolling to it first. */
fun scrollAndClick(matcher: Matcher<View>) {
    Espresso.onView(matcher).perform(ViewActions.scrollTo(), ViewActions.click())
}

/** Matcher for a view whose text equals the localized value of a string
 * resource as resolved by the view's OWN app context (works for any device
 * locale, unlike Espresso's withText(int) which may resolve against a
 * different configuration). */
fun withAppText(@StringRes resId: Int): Matcher<View> =
    object : TypeSafeMatcher<View>() {
        override fun matchesSafely(view: View): Boolean {
            val expected = view.context.getString(resId)
            return ((view as? android.widget.TextView)?.text?.toString()) == expected
        }

        override fun describeTo(description: Description) {
            description.appendText("with text of string resource $resId")
        }
    }

/** Matcher for the home start/stop control label in the active locale. */
val startStopLabel: Matcher<View> = withAppText(com.visionrt.feature.R.string.home_start)

/** Flow used right after a fresh launch: training step -> disclaimer -> ack. */
fun doAcknowledge() {
    scrollAndClick(withId(com.visionrt.feature.R.id.continue_button))
    scrollAndClick(withId(com.visionrt.feature.R.id.ack_button))
    Espresso.onView(startStopLabel).check(ViewAssertions.matches(isDisplayed()))
}

/** Asserts no visible view carries the given string resource in the locale
 * resolved by the root view's own context (any device locale). */
fun assertNoText(@StringRes resId: Int) {
    Espresso.onView(ViewMatchers.isRoot()).perform(AssertNoTextAction(resId))
}

private class AssertNoTextAction(@StringRes private val resId: Int) : ViewAction {
    override fun getConstraints(): Matcher<View> = ViewMatchers.isRoot()

    override fun getDescription(): String = "assert no view has resource text $resId"

    override fun perform(uiController: UiController, view: View) {
        val expected = view.context.getString(resId)
        var found = false

        fun walk(v: View) {
            if (v is android.widget.TextView && v.text?.toString() == expected) {
                found = true
            }
            (v as? android.view.ViewGroup)?.let { group ->
                repeat(group.childCount) { walk(group.getChildAt(it)) }
            }
        }

        walk(view)
        assertFalse("Found disallowed label '$expected'", found)
    }
}

/** Asserts the control meets the 48x48 dp minimum touch target (UX-003). */
fun hasMinTouchTarget(minWidthDp: Int, minHeightDp: Int): Matcher<View> =
    object : TypeSafeMatcher<View>() {
        override fun matchesSafely(view: View): Boolean {
            val density = view.resources.displayMetrics.density
            return view.width >= (minWidthDp * density).toInt() &&
                view.height >= (minHeightDp * density).toInt()
        }

        override fun describeTo(description: Description) {
            description.appendText("touch target >= ${minWidthDp}x${minHeightDp} dp")
        }
    }
