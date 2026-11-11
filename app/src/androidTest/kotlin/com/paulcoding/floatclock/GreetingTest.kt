package com.paulcoding.floatclock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulcoding.floatclock.ui.theme.FloatClockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GreetingTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun greeting_isDisplayed() {
    composeTestRule.setContent { FloatClockTheme { Greeting("Android") } }
    composeTestRule.onNodeWithText("Hello Android!").assertIsDisplayed()
  }
}
