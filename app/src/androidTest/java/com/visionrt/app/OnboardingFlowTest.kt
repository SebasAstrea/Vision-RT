package com.visionrt.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.visionrt.app.testutil.assertNoText
import com.visionrt.app.testutil.doAcknowledge
import com.visionrt.app.testutil.hasMinTouchTarget
import com.visionrt.app.testutil.enableAccessibilityChecks
import com.visionrt.app.testutil.resetSettingsState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.UninstallModules
import com.visionrt.data.di.SettingsModule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M1 gate: the safety disclaimer cannot be bypassed (FR-001) and the primary
 * start/stop control meets the 48x48 dp touch target on home (FR-004,
 * UX-003).
 */
@HiltAndroidTest
@UninstallModules(SettingsModule::class)
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun resetState() {
        resetSettingsState()
        enableAccessibilityChecks()
    }

    @Test
    fun homeIsNotReachableWithoutAcknowledgment() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            assertNoText(com.visionrt.feature.R.string.home_start)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun startControlIsLargeAndVisibleAfterAcknowledgment() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            doAcknowledge()
            Espresso.onView(ViewMatchers.withId(com.visionrt.feature.R.id.start_stop_button))
                .check(
                    ViewAssertions.matches(
                        hasMinTouchTarget(minWidthDp = 48, minHeightDp = 48),
                    ),
                )
        } finally {
            scenario.close()
        }
    }
}
