package com.visionrt.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.visionrt.app.testutil.click
import com.visionrt.app.testutil.doAcknowledge
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
 * M1 guarantee (FR-012): the verbosity selection is persisted and honored
 * after an app relaunch, and an acknowledged disclaimer keeps the user on the
 * home screen (FR-001.3).
 */
@HiltAndroidTest
@UninstallModules(SettingsModule::class)
@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun resetState() {
        resetSettingsState()
        enableAccessibilityChecks()
    }

    @Test
    fun verbositySelectionSurvivesRelaunch() {
        val first = ActivityScenario.launch(MainActivity::class.java)
        doAcknowledge()
        scrollAndClick(ViewMatchers.withId(com.visionrt.feature.R.id.settings_button))
        click(ViewMatchers.withId(com.visionrt.feature.R.id.verbosity_detailed))
        Espresso.pressBack()
        first.close()

        val second = ActivityScenario.launch(MainActivity::class.java)
        try {
            // Acknowledgment still set: the app must land on home, not onboarding.
            Espresso.onView(com.visionrt.app.testutil.startStopLabel)
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            scrollAndClick(ViewMatchers.withId(com.visionrt.feature.R.id.settings_button))
            Espresso.onView(ViewMatchers.withId(com.visionrt.feature.R.id.verbosity_detailed))
                .check(ViewAssertions.matches(ViewMatchers.isChecked()))
        } finally {
            second.close()
        }
    }
}
