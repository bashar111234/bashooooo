package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivitySmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launchesAndDisplaysLoginScreen() {
        composeTestRule.onNodeWithTag("card_number_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pricing_plan_row_0").performScrollTo().assertIsDisplayed()
    }
}
