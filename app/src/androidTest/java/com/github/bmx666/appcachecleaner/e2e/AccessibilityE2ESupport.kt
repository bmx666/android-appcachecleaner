package com.github.bmx666.appcachecleaner.e2e

import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
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

    /**
     * CRITICAL: by default UiAutomation suppresses every OTHER accessibility service while the
     * test holds it - which unbinds our AppCacheCleanerService, so it never binds and a clean
     * silently no-ops. Tell UiAutomator to keep other services alive. Must be set BEFORE the
     * first UiDevice access (it fixes the flags the UiAutomation bridge is created with). The
     * flag only exists / takes effect on API 24+; on API 23 UiAutomation always suppresses, so
     * these E2E tests cannot run there.
     */
    private fun allowOtherAccessibilityServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Configurator.getInstance().setUiAutomationFlags(
                UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        }
    }

    // Routed through UiDevice so it uses the (non-suppressing) UiAutomation configured above,
    // not a second flags-0 automation that would re-suppress our service.
    private fun shell(cmd: String) {
        device.executeShellCommand(cmd)
    }

    /**
     * Enable accessibility + usage-stats so the app behaves as it would for a fully set-up user,
     * then BLOCK until the service is genuinely bound. Setting the secure flag only makes
     * PermissionChecker (which reads settings) report "granted" - the system binds the service
     * asynchronously, and a clean started before the bind silently no-ops (onClearCache never
     * fires), which is exactly the "press clean, nothing happens, back to home" symptom.
     */
    fun grantPrerequisites() {
        allowOtherAccessibilityServices() // before any UiDevice/shell use
        shell("appops set ${targetContext.packageName} GET_USAGE_STATS allow")
        shell("settings put secure enabled_accessibility_services ${serviceComponent()}")
        shell("settings put secure accessibility_enabled 1")
        check(awaitServiceBound()) {
            "accessibility service was not bound within ${SERVICE_BIND_MS}ms"
        }
    }

    /**
     * Poll the live AccessibilityManager until our service shows up as actually enabled/bound.
     * Match on AccessibilityServiceInfo.getId() (the flattened "pkg/cls" component string, stable
     * since API 14) - resolveInfo/serviceInfo can be null or carry a mismatched packageName across
     * API levels, which made the old packageName check report "not bound" even when it was (the
     * API 23..latest grantPrerequisites failure).
     */
    private fun awaitServiceBound(): Boolean {
        val am = targetContext.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as AccessibilityManager
        val want = serviceComponent()
        val wantClass = AppCacheCleanerService::class.java.name
        val deadline = SystemClock.uptimeMillis() + SERVICE_BIND_MS
        while (SystemClock.uptimeMillis() < deadline) {
            val bound = am.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.id == want || it.id?.endsWith(wantClass) == true }
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

        // All 4 boxes must be checked IN ORDER (each enables the next); only then does OK enable.
        // Click strictly by index 0..3 and tap the row's bounds-centre so the hit lands on the
        // Row's `clickable` (the inner Checkbox has onCheckedChange=null, so isChecked is not a
        // reliable click target). Re-query each pass because handles go stale after recomposition.
        for (i in 0 until 4) {
            val rows = device.findObjects(By.checkable(true))
                .sortedBy { it.visibleBounds.centerY() }
            val row = rows.getOrNull(i) ?: break
            if (!row.isChecked) {
                val b = row.visibleBounds
                device.click(b.centerX(), b.centerY())
                device.waitForIdle()
            }
        }

        // OK is enabled only once every box is checked - wait for the enabled node before tapping.
        val ok = targetContext.getString(android.R.string.ok)
        device.wait(Until.findObject(By.text(ok).enabled(true)), FIRST_BOOT_MS)?.click()
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
