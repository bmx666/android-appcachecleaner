package com.github.bmx666.appcachecleaner.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.e2e.AccessibilityE2ESupport.device
import com.github.bmx666.appcachecleaner.e2e.AccessibilityE2ESupport.targetContext
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-device end-to-end: do exactly what a user does - open the app, pick the user apps,
 * select them all, hit Clear cache, then let the live AccessibilityService walk the real
 * system "App info" screens for every package and come back.
 *
 * The single thing that MUST be true at the end: we land back on the main screen showing the
 * result ("Clean cache was finished ... Cleaned up <size>"). The size may be 0 B (caches were
 * already empty, or some apps - Chrome, this app itself - get skipped); that is a valid result,
 * not a failure. We assert reaching the result, never a specific byte count.
 *
 * Run: ./gradlew :app:connectedDebugAndroidTest  (needs a connected device/emulator).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CleanCacheE2ETest {

    @Before fun setUp() {
        AccessibilityE2ESupport.grantPrerequisites()
        AccessibilityE2ESupport.launchApp()
    }

    @After fun tearDown() {
        AccessibilityE2ESupport.revokePrerequisites()
        device.pressHome()
    }

    @Test fun userPicksAppsRunsCleanAndReachesResult() {
        // 1. Home -> "Clean cache of user apps"
        val userApps = targetContext.getString(R.string.btn_clean_cache_user_apps)
        val userAppsBtn = device.wait(
            Until.findObject(By.text(userApps)), AccessibilityE2ESupport.APP_READY_MS)
        assertNotNull("main screen / user-apps button not shown", userAppsBtn)
        userAppsBtn.click()

        // 2. Package list -> "Check all apps", then "Clear cache" (icons -> contentDescription)
        val checkAll = targetContext.getString(R.string.description_apps_all_check)
        val checkAllBtn = device.wait(
            Until.findObject(By.desc(checkAll)), AccessibilityE2ESupport.APP_READY_MS)
        assertNotNull("package list / check-all not shown (list still loading?)", checkAllBtn)
        checkAllBtn.click()

        val clearCache = targetContext.getString(R.string.clear_cache_btn_text)
        val clearBtn = device.wait(
            Until.findObject(By.desc(clearCache)), AccessibilityE2ESupport.APP_READY_MS)
        assertNotNull("clear-cache action not shown", clearBtn)
        clearBtn.click()

        // 3. The service now drives the real system screens. Wait for the result title to come
        //    back on the main screen. "Clean cache was finished" is the stable prefix shown both
        //    while computing the size and once the cleaned size (incl. 0 B) is displayed.
        val finishPrefix =
            AccessibilityE2ESupport.titlePrefix(R.string.text_clean_cache_finish_display_size)
        val result = device.wait(
            Until.findObject(By.textContains(finishPrefix)), AccessibilityE2ESupport.RESULT_MS)
        assertNotNull(
            "did not reach the finished result screen (prefix: \"$finishPrefix\")", result)
    }
}
