package com.github.bmx666.appcachecleaner.e2e

import android.app.Instrumentation
import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.service.AppCacheCleanerService

/**
 * Real-device E2E plumbing. These tests run on an actual phone/emulator (connected*AndroidTest),
 * NOT Robolectric - they drive the live UI and let the live AccessibilityService walk the real
 * system "App info" screens.
 *
 * The two prerequisites a human normally taps through are granted here via the instrumentation
 * shell (runs as the `shell` uid, no root):
 *  - the Accessibility service must be enabled (secure settings) AND actually BOUND,
 *  - GET_USAGE_STATS must be allowed (AppOps) or the main screen keeps the Clean buttons disabled.
 *
 * Plus the one-time first-boot consent screen (4 sequential checkboxes + OK) is dismissed.
 */
object AccessibilityE2ESupport {

    // A full clean walks N system screens; be generous so slow devices / large app lists pass.
    const val APP_READY_MS = 15_000L
    const val SERVICE_BIND_MS = 15_000L
    const val FIRST_BOOT_MS = 5_000L
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

    /**
     * Enable accessibility + usage-stats so the app behaves as it would for a fully set-up user,
     * then BLOCK until the service is genuinely bound. Setting the secure flag only makes
     * PermissionChecker (which reads settings) report "granted" - the system binds the service
     * asynchronously, and a clean started before the bind silently no-ops (onClearCache never
     * fires), which is exactly the "press clean, nothing happens, back to home" symptom.
     */
    fun grantPrerequisites() {
        shell("appops set ${targetContext.packageName} GET_USAGE_STATS allow")
        shell("settings put secure enabled_accessibility_services ${serviceComponent()}")
        shell("settings put secure accessibility_enabled 1")
        check(awaitServiceBound()) {
            "accessibility service was not bound within ${SERVICE_BIND_MS}ms"
        }
    }

    /** Poll the live AccessibilityManager until our service shows up as actually enabled/bound. */
    private fun awaitServiceBound(): Boolean {
        val am = targetContext.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as AccessibilityManager
        val deadline = SystemClock.uptimeMillis() + SERVICE_BIND_MS
        while (SystemClock.uptimeMillis() < deadline) {
            val bound = am.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo?.serviceInfo?.packageName == targetContext.packageName }
            if (bound) return true
            SystemClock.sleep(200)
        }
        return false
    }

    /** Leave the device clean for the next test / the user. */
    fun revokePrerequisites() {
        shell("settings put secure accessibility_enabled 0")
        shell("settings delete secure enabled_accessibility_services")
    }

    /** Cold-launch the app, wait for it, then clear the one-time first-boot consent screen. */
    fun launchApp() {
        device.pressHome()
        val intent = targetContext.packageManager
            .getLaunchIntentForPackage(targetContext.packageName)!!
            .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        targetContext.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(targetContext.packageName).depth(0)), APP_READY_MS)
        dismissFirstBootIfPresent()
    }

    /**
     * First launch shows a consent screen: 4 checkboxes that enable sequentially (each unlocks
     * the next) then an OK button that only enables once all are checked. No-op on later runs.
     */
    private fun dismissFirstBootIfPresent() {
        val title = targetContext.getString(R.string.first_boot_title)
        if (device.wait(Until.hasObject(By.text(title)), FIRST_BOOT_MS) != true) return

        // Tick all checkboxes top-to-bottom; clicking one enables the next within the same pass.
        repeat(4) {
            val boxes = device.findObjects(By.checkable(true))
                .sortedBy { it.visibleBounds.centerY() }
            val next = boxes.firstOrNull { !it.isChecked } ?: return@repeat
            next.click()
            device.waitForIdle()
        }

        val ok = targetContext.getString(android.R.string.ok)
        device.wait(Until.findObject(By.text(ok)), FIRST_BOOT_MS)?.click()
        // Back on the splash->home gate; wait for it to settle.
        device.waitForIdle()
    }

    /** Stable, locale-independent prefix of a result title (text is translatable="false"). */
    fun titlePrefix(resId: Int): String =
        targetContext.getString(resId, "").substringBefore("\n").trim()

    /** First-boot helper exposed for clarity in case a test needs to re-check state. */
    @Suppress("unused")
    fun checkedBoxes(): List<UiObject2> =
        device.findObjects(By.checkable(true)).filter { it.isChecked }
}
