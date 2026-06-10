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
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.service.AppCacheCleanerService

/**
 * Real-device E2E plumbing. These tests run on an actual phone/emulator (connected*AndroidTest),
 * NOT Robolectric - they drive the live UI and let the live AccessibilityService walk the real
 * system "App info" screens.
 *
 * Accessibility is NOT force-enabled from the command line. Instead the test grants it the way a
 * user does: tap Allow on the in-app dialog, then flip the service ON inside the system
 * Accessibility settings (driven by UiAutomator). Usage-stats (not an accessibility permission)
 * is still granted via AppOps so the main-screen Clean buttons can be reached.
 *
 * One thing IS forced, and must be: UiAutomation suppresses every OTHER accessibility service by
 * default while a test holds it - which would unbind our service the moment we enable it. We tell
 * UiAutomator not to do that (API 24+; on API 23 it always suppresses, so these tests can't run
 * there).
 */
object AccessibilityE2ESupport {

    // A full clean walks N system screens; be generous so slow devices / large app lists pass.
    const val APP_READY_MS = 15_000L
    const val SERVICE_BIND_MS = 15_000L
    const val FIRST_BOOT_MS = 5_000L
    const val DIALOG_MS = 4_000L
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
        device.executeShellCommand(cmd)
    }

    /**
     * Keep OTHER accessibility services alive while the test runs (see class doc). Must be set
     * before the first UiDevice access - it fixes the flags the UiAutomation bridge is built with.
     */
    private fun allowOtherAccessibilityServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Configurator.getInstance().setUiAutomationFlags(
                UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        }
    }

    /** Environment-only setup: no accessibility forcing here, just usage-stats + the no-suppress flag. */
    fun prepareEnvironment() {
        allowOtherAccessibilityServices() // before any UiDevice/shell use
        shell("appops set ${targetContext.packageName} GET_USAGE_STATS allow")
    }

    /** Re-runs leave the service enabled (idempotent) - nothing to undo without shell-forcing a11y. */
    fun revokePrerequisites() {
        // intentionally a no-op
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
     * First launch shows a consent screen (FirstBootScreen): 4 checkboxes that enable sequentially
     * (each unlocks the next) then an OK button enabled only once all are checked.
     *
     * Each checkbox is a StyledLabelledCheckBox: a clickable Compose Row (clickable=enabled) that
     * MERGES its children into ONE semantics node whose TEXT is the label string
     * (first_boot_confirm_1..4). The inner Checkbox(onCheckedChange=null) exposes no toggle of its
     * own, so we drive the ROW - matched by a distinctive, stable substring of each label, in order
     * (a still-disabled row won't be clickable, but by the time we reach it the prior tick enabled
     * it). The Column verticalScroll()s, so later rows / the OK button may be off-screen: we scroll
     * each target into view first.
     *
     * Detection matches the title by PREFIX, not exact text: the title Text is maxLines=1 +
     * Ellipsis, so on a narrow screen it renders truncated and an exact By.text() would miss.
     */
    private fun dismissFirstBootIfPresent() {
        val titlePrefix = targetContext.getString(R.string.first_boot_title).take(11) // "Please read"
        if (device.wait(Until.hasObject(By.textStartsWith(titlePrefix)), FIRST_BOOT_MS) != true) return

        // Distinctive, stable substrings of first_boot_confirm_1..4 (translatable=false), in order.
        val rowMarkers = listOf(
            "read the chapter",
            "alternative text for searching",
            "Xiaomi MIUI",
            "Submit a bug report",
        )
        rowMarkers.forEach { marker -> tickFirstBootRow(marker) }

        // OK is enabled only once every box is checked - scroll it in, wait for enabled, tap.
        val ok = targetContext.getString(android.R.string.ok)
        runCatching {
            UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().text(ok))
        }
        device.wait(Until.findObject(By.text(ok).enabled(true)), FIRST_BOOT_MS)?.click()
        device.waitForIdle()
    }

    /** Scroll the consent row carrying [marker] into view (if needed) and tap it to tick its box. */
    private fun tickFirstBootRow(marker: String) {
        var row = device.wait(Until.findObject(By.textContains(marker)), FIRST_BOOT_MS)
        if (row == null) {
            runCatching {
                UiScrollable(UiSelector().scrollable(true))
                    .scrollIntoView(UiSelector().textContains(marker))
            }
            row = device.wait(Until.findObject(By.textContains(marker)), FIRST_BOOT_MS)
        }
        row?.click() // clicks the merged clickable Row -> onCheckedChange(!checked)
        device.waitForIdle()
    }

    /**
     * If tapping a Clean button raised the "Enable Accessibility" dialog, grant it like a user:
     * tap Allow -> in system Accessibility settings open our service ("Cache Cleaner") and turn the
     * switch ON (confirming the system warning) -> return to the app. Returns true if it performed
     * the grant (caller should re-tap the Clean button), false if no dialog was shown (already
     * granted). Blocks until the service is genuinely bound.
     */
    fun grantAccessibilityViaDialogIfShown(): Boolean {
        val title = targetContext.getString(R.string.text_enable_accessibility_title)
        if (device.wait(Until.hasObject(By.text(title)), DIALOG_MS) != true) return false

        val allow = targetContext.getString(R.string.allow)
        device.wait(Until.findObject(By.text(allow)), DIALOG_MS)?.click()
        device.waitForIdle()

        enableServiceInSystemSettings()

        check(awaitServiceBound()) {
            "service not bound after enabling it in system Accessibility settings"
        }
        return true
    }

    /** OEM-fragile: open our entry in system Accessibility settings and flip the switch on. */
    private fun enableServiceInSystemSettings() {
        // We should have left the app for the Settings app.
        device.wait(Until.gone(By.pkg(targetContext.packageName).depth(0)), APP_READY_MS)

        val label = targetContext.getString(R.string.service_name) // "Cache Cleaner"
        var entry = device.wait(Until.findObject(By.text(label)), APP_READY_MS)
        if (entry == null) {
            runCatching {
                UiScrollable(UiSelector().scrollable(true)).scrollTextIntoView(label)
            }
            entry = device.wait(Until.findObject(By.text(label)), APP_READY_MS)
        }
        entry?.click()
        device.waitForIdle()

        // Flip the on/off switch if it is not already on.
        val sw = device.wait(Until.findObject(By.clazz("android.widget.Switch")), APP_READY_MS)
            ?: device.wait(Until.findObject(By.checkable(true)), APP_READY_MS)
        if (sw != null && !sw.isChecked) {
            sw.click()
            device.waitForIdle()
            // Confirm the system's "allow full control" warning (label varies by OEM/locale).
            for (t in listOf("Allow", "ALLOW", "OK", "Got it", "Turn on", "Allow full control")) {
                val btn = device.wait(Until.findObject(By.text(t)), 1_500)
                if (btn != null) { btn.click(); device.waitForIdle(); break }
            }
        }

        // Walk back until the app is foreground again.
        repeat(4) {
            if (device.hasObject(By.pkg(targetContext.packageName).depth(0))) return
            device.pressBack()
            device.waitForIdle()
        }
    }

    /**
     * Poll the live AccessibilityManager until our service shows up as actually enabled/bound.
     * Match on AccessibilityServiceInfo.getId() (the flattened "pkg/cls" component string, stable
     * since API 14) - resolveInfo/serviceInfo can be null or carry a mismatched packageName.
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

    /** Stable, locale-independent prefix of a result title (text is translatable="false"). */
    fun titlePrefix(resId: Int): String =
        targetContext.getString(resId, "").substringBefore("\n").trim()
}
