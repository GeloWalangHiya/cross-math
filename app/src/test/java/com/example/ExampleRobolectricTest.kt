package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cross Math", appName)
    }

    @Test
    fun crossMathScreenRendersControlsAndAllowsDifficultySwitch() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("difficulty_EASY").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("difficulty_MEDIUM").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("difficulty_HARD").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("mute_toggle_button").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("hint_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("undo_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("swap_button").performScrollTo().assertIsDisplayed()

        // Switch to Easy, verify keypad input, Hint, and Undo
        composeTestRule.onNodeWithTag("difficulty_EASY").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("key_5").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("hint_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("undo_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mute_toggle_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }
}
