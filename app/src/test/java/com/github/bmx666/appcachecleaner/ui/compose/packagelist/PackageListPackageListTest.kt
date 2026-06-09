package com.github.bmx666.appcachecleaner.ui.compose.packagelist

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.bmx666.appcachecleaner.model.AppPackage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

// Robolectric-driven Compose smoke test for the package list. Asserts on text nodes only
// (labels / package names / empty-state string) which render deterministically.
// showCacheSize = false avoids the Coil icon + locale + SDK-gated formatter path.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PackageListPackageListTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun pkg(name: String, label: String) =
        AppPackage(
            name = name,
            label = label,
            locale = Locale.US,
            cacheBytes = 0L,
            visible = true,
            ignore = false,
        )

    @Test
    fun `empty list shows the no-matches message`() {
        composeRule.setContent {
            MaterialTheme {
                PackageListPackageList(
                    pkgList = emptyList(),
                    checkedNames = emptySet(),
                    showCacheSize = false,
                    onAppIconClick = {},
                    onAppIconLongClick = {},
                    onPackageClick = { _, _ -> },
                )
            }
        }

        // R.string.message_no_matches_found resolves via Robolectric resources.
        composeRule.onNodeWithText("No matches found", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun `renders labels and package names`() {
        composeRule.setContent {
            MaterialTheme {
                PackageListPackageList(
                    pkgList = listOf(
                        pkg("com.alpha", "Alpha"),
                        pkg("com.beta", "Beta"),
                    ),
                    checkedNames = emptySet(),
                    showCacheSize = false,
                    onAppIconClick = {},
                    onAppIconLongClick = {},
                    onPackageClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onNodeWithText("Beta").assertIsDisplayed()
        composeRule.onNodeWithText("com.alpha").assertIsDisplayed()
        composeRule.onNodeWithText("com.beta").assertIsDisplayed()
    }

    @Test
    fun `tapping a row reports the selection`() {
        var clicked: Pair<String, Boolean>? = null

        composeRule.setContent {
            MaterialTheme {
                PackageListPackageList(
                    pkgList = listOf(pkg("com.alpha", "Alpha")),
                    checkedNames = emptySet(),
                    showCacheSize = false,
                    onAppIconClick = {},
                    onAppIconLongClick = {},
                    onPackageClick = { p, checked -> clicked = p.name to checked },
                )
            }
        }

        composeRule.onNodeWithText("Alpha").performClick()
        composeRule.waitForIdle()

        assertEquals("com.alpha" to true, clicked)
    }
}
