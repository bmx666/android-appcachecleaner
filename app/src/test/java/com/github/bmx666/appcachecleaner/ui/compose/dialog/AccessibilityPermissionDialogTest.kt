package com.github.bmx666.appcachecleaner.ui.compose.dialog

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.bmx666.appcachecleaner.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// "Allow accessibility if required": when permission is missing the main screen pops this
// dialog. Robolectric-driven Compose smoke test - render it and verify both buttons report
// the dismiss. Tapping Allow also fires off the system accessibility-settings Intent; that
// launch is irrelevant here and tolerated (try/catch) because onDismiss runs first.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityPermissionDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `renders the prompt and Deny dismisses it`() {
        var dismissed = 0

        composeRule.setContent {
            MaterialTheme {
                AccessibilityPermissionDialog(showDialog = true) { dismissed++ }
            }
        }

        // assertExists (not assertIsDisplayed): inside a Dialog window the viewport/layout
        // bounds are unreliable under Robolectric, but presence + click still work.
        composeRule.onNodeWithText(context.getString(R.string.text_enable_accessibility_title))
            .assertExists()
        composeRule.onNodeWithText(context.getString(R.string.allow)).assertExists()

        composeRule.onNodeWithText(context.getString(R.string.deny)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, dismissed)
    }

    @Test
    fun `Allow dismisses before opening accessibility settings`() {
        var dismissed = 0

        composeRule.setContent {
            MaterialTheme {
                AccessibilityPermissionDialog(showDialog = true) { dismissed++ }
            }
        }

        // Allow runs onDismiss() then launches the settings Intent; the launch is not under
        // test (and may have no resolver under Robolectric), so tolerate it.
        try {
            composeRule.onNodeWithText(context.getString(R.string.allow)).performClick()
            composeRule.waitForIdle()
        } catch (_: Throwable) {
        }

        assertEquals(1, dismissed)
    }
}
