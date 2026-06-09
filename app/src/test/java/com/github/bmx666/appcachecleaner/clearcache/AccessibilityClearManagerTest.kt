package com.github.bmx666.appcachecleaner.clearcache

import com.github.bmx666.appcachecleaner.clearcache.scenario.DefaultClearScenario
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_SYSTEM
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_USER
import com.github.bmx666.appcachecleaner.fake.FakeNodeView
import com.github.bmx666.appcachecleaner.fake.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// End-to-end coordination of the clear-cache run, exactly as a user drives it: pick several
// packages, start the run, the manager opens each App Info screen, the screen reports its
// node tree (here scripted), the scenario clicks "Clear cache", and the run walks every
// package and finishes with a result.
//
// The real DefaultClearScenario runs against scripted FakeNodeView trees, so this exercises
// the actual button-finding + the manager's per-package lifecycle (open -> wait first event
// -> drive scenario -> next), skip rules, and cancellation taxonomy. Everything runs on a
// single test dispatcher (injected via DispatcherProvider) under virtual time -> fully
// deterministic, no real Main/IO pools, no Robolectric. Build.VERSION.SDK_INT defaults to 0
// here, so the O+ / API-34 branches are covered separately in AccessibilityClearManagerSdk34Test.
//
// The injected openAppInfo callback is where the test feeds each package's screen, mirroring
// production where opening App Info triggers the accessibility window-state event.
@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityClearManagerTest {

    private val dispatcher = StandardTestDispatcher()
    private val provider = TestDispatcherProvider(dispatcher)

    private fun cacheScenario() = DefaultClearScenario().apply {
        forceStopApps = false
        // MIN_WAIT_APP_PERFORM_CLICK_MS (2000) is the floor; keep the first-event wait short.
        maxWaitAppTimeoutMs = 2000
        maxWaitAccessibilityEventMs = 250
        arrayTextClearCacheButton.add("clear cache")
    }

    private fun manager(scenario: DefaultClearScenario = cacheScenario()) =
        AccessibilityClearManager(scenario, provider)

    // App Info screen exposing a "Clear cache" button (enabled+clickable by default).
    private fun clearCacheTree(enabled: Boolean = true, clickResult: Boolean = true) =
        FakeNodeView().child(
            FakeNodeView(
                className = "android.widget.Button",
                text = "Clear cache",
                isClickable = true,
                enabledState = enabled,
                clickResult = clickResult,
            )
        )

    @Test
    fun `clears every selected package and reaches the result with no error`() = runTest(dispatcher) {
        val mgr = manager()
        val pkgs = arrayListOf("com.alpha", "com.beta", "com.gamma")

        val positions = mutableListOf<Int>()
        var finishCount = 0
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            pkgs,
            updatePosition = { positions.add(it) },
            performBack = { true },
            getForegroundPackageName = { null },
            // every package opens App Info and shows a clear-cache button
            openAppInfo = { mgr.onWindowStateChanged(clearCacheTree()) },
            finish = { msg, _ -> finishCount++; finishMsg = msg },
        )
        advanceUntilIdle()

        // finished once, success (null message == "Clean cache finished" on the main screen)
        assertEquals(1, finishCount)
        assertEquals(null, finishMsg)
        // progress runs 0 .. N over the whole list
        assertEquals(0, positions.first())
        assertEquals(pkgs.size, positions.last())
    }

    @Test
    fun `disabled clear cache button still finishes the package - no panic`() = runTest(dispatcher) {
        // Cache already empty / not yet computed -> button disabled. The scenario must treat
        // that as "nothing to do, move on", not as a failure that aborts the run.
        val mgr = manager()
        var finishMsg: String? = "UNSET"
        var finishCount = 0

        mgr.clearCacheApp(
            arrayListOf("com.alpha", "com.beta"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = { mgr.onWindowStateChanged(clearCacheTree(enabled = false)) },
            finish = { msg, _ -> finishCount++; finishMsg = msg },
        )
        advanceUntilIdle()

        assertEquals(1, finishCount)
        assertEquals(null, finishMsg)
    }

    @Test
    fun `package that opens nothing is skipped and the run still finishes`() = runTest(dispatcher) {
        // A system / ignored package (e.g. one whose App Info never opens) emits no event:
        // it must time out and be skipped, never grabbing the previous package's stale node.
        val mgr = manager()
        val pkgs = arrayListOf("com.alpha", "com.silent", "com.gamma")
        val positions = mutableListOf<Int>()
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            pkgs,
            updatePosition = { positions.add(it) },
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = { pkg ->
                // com.silent intentionally feeds no screen -> first-event timeout -> skip
                if (pkg != "com.silent") mgr.onWindowStateChanged(clearCacheTree())
            },
            finish = { msg, _ -> finishMsg = msg },
        )
        advanceUntilIdle()

        assertEquals(null, finishMsg)
        assertEquals(pkgs.size, positions.last())
    }

    @Test
    fun `empty package name is skipped without opening App Info`() = runTest(dispatcher) {
        val mgr = manager()
        val pkgs = arrayListOf("com.alpha", "", "com.gamma")
        val opened = mutableListOf<String>()
        val positions = mutableListOf<Int>()
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            pkgs,
            updatePosition = { positions.add(it) },
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = { pkg -> opened.add(pkg); mgr.onWindowStateChanged(clearCacheTree()) },
            finish = { msg, _ -> finishMsg = msg },
        )
        advanceUntilIdle()

        assertEquals(null, finishMsg)
        assertTrue("blank package must not open App Info", "" !in opened)
        assertEquals(pkgs.size, positions.last())
    }

    @Test
    fun `interrupt by user reports the interrupt message and current package`() = runTest(dispatcher) {
        // Models tapping the overlay stop icon mid-run (AccessibilityOverlay -> interruptByUser).
        val mgr = manager()
        var finishMsg: String? = "UNSET"
        var finishPkg: String? = "UNSET"

        mgr.clearCacheApp(
            arrayListOf("com.alpha", "com.beta"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            // feed nothing -> the run parks waiting for com.alpha's first event
            openAppInfo = {},
            finish = { msg, pkg -> finishMsg = msg; finishPkg = pkg },
        )
        runCurrent() // advance to the first-event wait on com.alpha
        mgr.interruptByUser()
        advanceUntilIdle()

        // surfaced on the main screen as "Clean cache process was interrupted"
        assertEquals(CANCEL_INTERRUPTED_BY_USER.message, finishMsg)
        assertEquals("com.alpha", finishPkg)
    }

    @Test
    fun `interrupt by system reports the system-interrupt message`() = runTest(dispatcher) {
        val mgr = manager()
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            arrayListOf("com.alpha"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = {},
            finish = { msg, _ -> finishMsg = msg },
        )
        runCurrent()
        mgr.interruptBySystem()
        advanceUntilIdle()

        assertEquals(CANCEL_INTERRUPTED_BY_SYSTEM.message, finishMsg)
    }

    @Test
    fun `a new run preempts the in-flight one without firing its finish`() = runTest(dispatcher) {
        // Starting a second clear while the first is still waiting cancels the first with
        // CANCEL_INIT (silent: no finish), and only the second run reports a result.
        val mgr = manager()
        var firstFinish = 0
        var secondFinish = 0
        var secondMsg: String? = "UNSET"

        mgr.clearCacheApp(
            arrayListOf("com.first"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = {}, // parks waiting
            finish = { _, _ -> firstFinish++ },
        )
        runCurrent()

        mgr.clearCacheApp(
            arrayListOf("com.second"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = { mgr.onWindowStateChanged(clearCacheTree()) },
            finish = { msg, _ -> secondFinish++; secondMsg = msg },
        )
        advanceUntilIdle()

        assertEquals("preempted run must stay silent (CANCEL_INIT)", 0, firstFinish)
        assertEquals(1, secondFinish)
        assertEquals(null, secondMsg)
    }
}
