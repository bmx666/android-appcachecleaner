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
 * Real-device end-to-end interruption: start a full clean, then tap the floating overlay STOP
 * button the service shows while it works. The run must abort and the main screen must report
 * "Clean cache was interrupted".
 *
 * The overlay is a TYPE_ACCESSIBILITY_OVERLAY window owned by the service; its button has the
 * resource id `overlayButton` (see res/layout/accessibility_overlay_layout.xml), so we target
 * it by resource id rather than text.
 *
 * Run: ./gradlew :app:connectedDebugAndroidTest  (needs a connected device/emulator).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CleanCacheInterruptE2ETest {

    @Before fun setUp() {
        AccessibilityE2ESupport.grantPrerequisites()
        AccessibilityE2ESupport.launchApp()
    }

    @After fun tearDown() {
        AccessibilityE2ESupport.revokePrerequisites()
        device.pressHome()
    }

    @Test fun tappingOverlayStopInterruptsTheRun() {
        // Start the same run as the happy-path test.
        val userApps = targetContext.getString(R.string.btn_clean_cache_user_apps)
        device.wait(Until.findObject(By.text(userApps)), AccessibilityE2ESupport.APP_READY_MS)
            ?.click()

        val checkAll = targetContext.getString(R.string.description_apps_all_check)
        device.wait(Until.findObject(By.desc(checkAll)), AccessibilityE2ESupport.APP_READY_MS)
            ?.click()
        device.waitForIdle() // let the selection register before we fire Clear cache

        val clearCache = targetContext.getString(R.string.clear_cache_btn_text)
        device.wait(Until.findObject(By.desc(clearCache)), AccessibilityE2ESupport.APP_READY_MS)
            ?.click()

        // Mid-run the service shows the overlay stop button. Grab it and tap to interrupt.
        val overlayBtn = device.wait(
            Until.findObject(By.res(targetContext.packageName, "overlayButton")),
            AccessibilityE2ESupport.OVERLAY_MS)
        assertNotNull("overlay stop button never appeared - did the run start?", overlayBtn)
        overlayBtn.click()

        // The interruption must round-trip back to the main screen as the interrupted result.
        val interruptPrefix =
            AccessibilityE2ESupport.titlePrefix(R.string.text_clean_cache_interrupt_display_size)
        val result = device.wait(
            Until.findObject(By.textContains(interruptPrefix)), AccessibilityE2ESupport.RESULT_MS)
        assertNotNull(
            "interruption did not reach the main screen (prefix: \"$interruptPrefix\")", result)
    }
}
