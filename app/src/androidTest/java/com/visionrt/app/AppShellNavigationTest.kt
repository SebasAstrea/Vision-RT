package com.visionrt.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.visionrt.app.testutil.click
import com.visionrt.app.testutil.doAcknowledge
import com.visionrt.app.testutil.hasMinTouchTarget
import com.visionrt.app.testutil.enableAccessibilityChecks
import com.visionrt.app.testutil.resetSettingsState
import com.visionrt.app.testutil.scrollAndClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.UninstallModules
import com.visionrt.data.di.SettingsModule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M1 shell navigation (FR-003.4): settings and help are reachable from home
 * and the user can always return home. Accessibility checks run on every
 * screen via the enabled Espresso accessibility scanner.
 */
@HiltAndroidTest
@UninstallModules(SettingsModule::class)
@RunWith(AndroidJUnit4::class)
class AppShellNavigationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun resetState() {
        resetSettingsState()
        enableAccessibilityChecks()
    }

    @Test
    fun homeToSettingsAndBack() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            doAcknowledge()

            scrollAndClick(ViewMatchers.withId(com.visionrt.feature.R.id.settings_button))
            Espresso.onView(ViewMatchers.withId(com.visionrt.feature.R.id.verbosity_group))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.pressBack()
            Espresso.onView(com.visionrt.app.testutil.startStopLabel)
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun homeToHelpAndExplicitBackButtonReturnsHome() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            doAcknowledge()

            scrollAndClick(ViewMatchers.withId(com.visionrt.feature.R.id.help_button))
            Espresso.onView(ViewMatchers.withId(com.visionrt.feature.R.id.help_body))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            scrollAndClick(ViewMatchers.withId(com.visionrt.feature.R.id.back_home_button))
            Espresso.onView(com.visionrt.app.testutil.startStopLabel)
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        } finally {
            scenario.close()
        }
    }
}
