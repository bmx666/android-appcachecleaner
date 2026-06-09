package com.github.bmx666.appcachecleaner.e2e

import android.app.Instrumentation
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.github.bmx666.appcachecleaner.service.AppCacheCleanerService

/**
 * Real-device E2E plumbing. These tests run on an actual phone/emulator (connected*AndroidTest),
 * NOT Robolectric - they drive the live UI and let the live AccessibilityService walk the real
 * system "App info" screens.
 *
 * The two prerequisites a human normally taps through are granted here via the instrumentation
 * shell (runs as the `shell` uid, no root):
 *  - the Accessibility service must be enabled (secure settings),
 *  - GET_USAGE_STATS must be allowed (AppOps) or the main screen keeps the Clean buttons disabled.
 */
object AccessibilityE2ESupport {

    // A full clean walks N system screens; be generous so slow devices / large app lists pass.
    const val APP_READY_MS = 15_000L
    const val SERVICE_BIND_MS = 10_000L
    const val RESULT_MS = 180_000L
    const val OVERLAY_MS = 60_000L

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    /** Application under test (its packageName is the applicationId, e.g. ...debug). */
    val targetContext: Context get() = instrumentation.targetContext

    val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private fun serviceComponent(): String =
        targetContext.packageName + "/" + AppCacheCleanerService::class.java.name

    private fun shell(cmd: String) {
        val fd = instrumentation.uiAutomation.executeShellCommand(cmd)
        // Drain the pipe so the command actually completes before we move on.
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
    }

    /** Enable accessibility + usage-stats so the app behaves as it would for a fully set-up user. */
    fun grantPrerequisites() {
        shell("appops set ${targetContext.packageName} GET_USAGE_STATS allow")
        shell("settings put secure enabled_accessibility_services ${serviceComponent()}")
        shell("settings put secure accessibility_enabled 1")
        // Give the system a moment to bind the freshly-enabled service.
        device.wait(Until.hasObject(By.pkg(targetContext.packageName)), SERVICE_BIND_MS)
    }

    /** Leave the device clean for the next test / the user. */
    fun revokePrerequisites() {
        shell("settings put secure accessibility_enabled 0")
        shell("settings delete secure enabled_accessibility_services")
    }

    /** Cold-launch the app and wait for the home screen (past the splash/settings gate). */
    fun launchApp() {
        device.pressHome()
        val intent = targetContext.packageManager
            .getLaunchIntentForPackage(targetContext.packageName)!!
            .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        targetContext.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(targetContext.packageName).depth(0)), APP_READY_MS)
    }

    /** Stable, locale-independent prefix of a result title (text is translatable="false"). */
    fun titlePrefix(resId: Int): String =
        targetContext.getString(resId, "").substringBefore("\n").trim()
}
