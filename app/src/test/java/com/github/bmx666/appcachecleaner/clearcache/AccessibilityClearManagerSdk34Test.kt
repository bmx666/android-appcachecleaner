package com.github.bmx666.appcachecleaner.clearcache

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.github.bmx666.appcachecleaner.clearcache.node.NodeView
import com.github.bmx666.appcachecleaner.clearcache.scenario.BaseClearScenario
import com.github.bmx666.appcachecleaner.clearcache.scenario.DefaultClearScenario
import com.github.bmx666.appcachecleaner.fake.FakeNodeView
import com.github.bmx666.appcachecleaner.fake.TestDispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK-gated coordination/scenario behaviour that the pure-JVM AccessibilityClearManagerTest
// cannot reach (Build.VERSION.SDK_INT == 0 there). Pinned to API 34 (UPSIDE_DOWN_CAKE):
//  - the Android 14 recomposition-dedup filter in onWindowStateChanged,
//  - the Android O+ clear-cache choreography (enabled-wait, post-click refresh, refresh-fail),
//  - go-back / self-detection driven through real setSettings(context) (which derives
//    selfPackageName + go-back cadence from prefs, like production).
// Still runs on a single injected test dispatcher under virtual time.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AccessibilityClearManagerSdk34Test {

    private val dispatcher = StandardTestDispatcher()
    private val provider = TestDispatcherProvider(dispatcher)
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    // Counts scenario invocations and always yields "keep waiting" so the manager feeds it
    // every event - lets the dedup filter be observed by call count.
    private class RecordingScenario : BaseClearScenario() {
        var cacheCalls = 0
        override fun resetInternalState() {}
        override suspend fun doClearCache(nodeInfo: NodeView): CancellationException? {
            cacheCalls++
            return null
        }
        override suspend fun doClearData(nodeInfo: NodeView): CancellationException? = null
    }

    private fun cacheScenario() = DefaultClearScenario().apply {
        forceStopApps = false
        maxWaitAppTimeoutMs = 2000
        maxWaitAccessibilityEventMs = 250
        arrayTextClearCacheButton.add("clear cache")
    }

    private fun clearCacheButtonTree(button: FakeNodeView) = FakeNodeView().child(button)

    @Test
    fun `identical recomposition events are ignored, a changed screen is processed`() = runTest(dispatcher) {
        val scenario = RecordingScenario()
        val mgr = AccessibilityClearManager(scenario, provider)

        // Same structure each call -> NodeState compares equal -> deduped.
        fun screenA() = FakeNodeView(className = "android.widget.FrameLayout", viewIdResourceName = "a")
        // Different className -> NodeState differs -> processed.
        fun screenB() = FakeNodeView(className = "android.widget.LinearLayout", viewIdResourceName = "b")

        mgr.clearCacheApp(
            arrayListOf("com.alpha"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = { mgr.onWindowStateChanged(screenA()) }, // first event
            finish = { _, _ -> },
        )
        runCurrent()
        assertEquals("first event processed", 1, scenario.cacheCalls)

        mgr.onWindowStateChanged(screenA()) // identical -> recomposition spam, ignored
        runCurrent()
        assertEquals("identical event must be ignored", 1, scenario.cacheCalls)

        mgr.onWindowStateChanged(screenB()) // real screen change -> processed
        runCurrent()
        assertEquals("changed screen processed", 2, scenario.cacheCalls)

        mgr.interruptByUser()
        advanceUntilIdle()
    }

    @Test
    fun `O+ enabled-wait then click finishes the package`() = runTest(dispatcher) {
        // Button starts disabled (cache still summing); a refresh enables it; the click then
        // greys it out (disableOnClick) so the post-click "still enabled?" guard is satisfied.
        val button = FakeNodeView(
            className = "android.widget.Button",
            text = "Clear cache",
            isClickable = true,
            enabledState = false,
            enableOnRefresh = true,
            disableOnClick = true,
            clickResult = true,
            refreshResult = true,
        )
        val mgr = AccessibilityClearManager(cacheScenario(), provider)
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            arrayListOf("com.alpha"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = { mgr.onWindowStateChanged(clearCacheButtonTree(button)) },
            finish = { msg, _ -> finishMsg = msg },
        )
        advanceUntilIdle()

        assertNull(finishMsg)
        assertTrue("button must be refreshed while waiting to enable", button.refreshCount >= 1)
        assertTrue("button must be clicked once enabled", button.clickCount >= 1)
    }

    @Test
    fun `O+ post-click refresh failure ends the package without re-clicking`() = runTest(dispatcher) {
        // Enabled+clickable from the start (no enabled-wait), click succeeds, but the
        // post-click refresh fails -> PACKAGE_FINISH_FAILED: package ends, run still finishes,
        // and the button is not clicked again.
        val button = FakeNodeView(
            className = "android.widget.Button",
            text = "Clear cache",
            isClickable = true,
            enabledState = true,
            clickResult = true,
            refreshResult = false,
        )
        val mgr = AccessibilityClearManager(cacheScenario(), provider)
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            arrayListOf("com.alpha"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { null },
            openAppInfo = { mgr.onWindowStateChanged(clearCacheButtonTree(button)) },
            finish = { msg, _ -> finishMsg = msg },
        )
        advanceUntilIdle()

        assertNull(finishMsg)
        assertEquals("must not retry the click after refresh failure", 1, button.clickCount)
        assertTrue(button.refreshCount >= 1)
    }

    @Test
    fun `go-back presses BACK until our app is foreground again`() = runTest(dispatcher) {
        // Real prefs: setSettings derives selfPackageName + go-back cadence (fires on the last
        // app). The foreground is Settings, then ours -> the manager presses BACK to return.
        val mgr = AccessibilityClearManager(cacheScenario(), provider)
        mgr.setSettings(context)

        val self = context.packageName
        // not-self twice, then self -> stop
        val foreground = ArrayDeque(listOf("com.android.settings", "com.android.settings", self))
        var backCount = 0
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            arrayListOf("com.alpha"), // single app -> index 0 is the last -> go-back runs
            updatePosition = {},
            performBack = { backCount++; true },
            getForegroundPackageName = { foreground.removeFirstOrNull() ?: self },
            // feed a neutral screen so the first event arrives (gotEvent == true -> go-back),
            // the real scenario finds nothing -> the package times out then go-back runs
            openAppInfo = { mgr.onWindowStateChanged(FakeNodeView(className = "x")) },
            finish = { msg, _ -> finishMsg = msg },
        )
        advanceUntilIdle()

        assertNull(finishMsg)
        assertTrue("must press BACK to return to our app", backCount >= 1)
    }

    @Test
    fun `clearing a list that includes our own package completes without error`() = runTest(dispatcher) {
        // Our own app (and apps like Chrome) can appear in the list; the self-package
        // force-stop guard means the run must not abort - it finishes cleanly.
        val mgr = AccessibilityClearManager(cacheScenario(), provider)
        mgr.setSettings(context)

        val self = context.packageName
        var finishMsg: String? = "UNSET"

        mgr.clearCacheApp(
            arrayListOf("com.alpha", self, "com.beta"),
            updatePosition = {},
            performBack = { true },
            getForegroundPackageName = { self },
            openAppInfo = { mgr.onWindowStateChanged(FakeNodeView(className = "x")) },
            finish = { msg, _ -> finishMsg = msg },
        )
        advanceUntilIdle()

        assertNull(finishMsg)
    }
}
