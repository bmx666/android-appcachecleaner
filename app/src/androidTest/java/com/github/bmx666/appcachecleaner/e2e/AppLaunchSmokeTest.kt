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
 * Real-device launch smoke test: open the app like the launcher does, get past the first-boot
 * consent, and require the Home screen to render. Guards against crashes during the first
 * composition after the splash (e.g. NavHost missing its NavigationEventDispatcherOwner), which
 * kill the process before any UI appears.
 *
 * Run: ./gradlew :app:connectedDebugAndroidTest  (needs a connected device/emulator, API 24+).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {

    @Before fun setUp() {
        AccessibilityE2ESupport.prepareEnvironment()
        AccessibilityE2ESupport.launchApp()
    }

    @After fun tearDown() {
        AccessibilityE2ESupport.revokePrerequisites()
        device.pressHome()
    }

    @Test fun launchReachesHomeScreen() {
        val userApps = targetContext.getString(R.string.btn_clean_cache_user_apps)
        assertNotNull(
            "Home screen not shown after launch (app crashed or splash never released)",
            device.wait(Until.findObject(By.text(userApps)), AccessibilityE2ESupport.APP_READY_MS))
    }
}
