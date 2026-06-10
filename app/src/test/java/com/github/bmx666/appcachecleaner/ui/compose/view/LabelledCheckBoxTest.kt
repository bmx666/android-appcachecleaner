package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric-driven Compose smoke test: renders the leaf composable in the JVM and
// verifies a single interaction. No device, no Hilt — the component is param + lambda only.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LabelledCheckBoxTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders label and toggles on click`() {
        var lastChange: Boolean? = null

        composeRule.setContent {
            MaterialTheme {
                LabelledCheckBox(
                    text = "Enable thing",
                    checked = false,
                    enabled = true,
                    onCheckedChange = { lastChange = it },
                )
            }
        }

        composeRule.onNodeWithText("Enable thing").assertIsDisplayed()

        composeRule.onNodeWithText("Enable thing").performClick()
        composeRule.waitForIdle()

        assertEquals(true, lastChange)
    }

    @Test
    fun `disabled checkbox swallows the click`() {
        var changed = false

        composeRule.setContent {
            MaterialTheme {
                LabelledCheckBox(
                    text = "Locked",
                    checked = false,
                    enabled = false,
                    onCheckedChange = { changed = true },
                )
            }
        }

        composeRule.onNodeWithText("Locked").performClick()
        composeRule.waitForIdle()

        assertEquals(false, changed)
    }
}
