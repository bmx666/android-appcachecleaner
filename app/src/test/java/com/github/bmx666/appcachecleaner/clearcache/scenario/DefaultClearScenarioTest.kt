package com.github.bmx666.appcachecleaner.clearcache.scenario

import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH_FAILED
import com.github.bmx666.appcachecleaner.fake.FakeNodeView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Pure-JVM scenario tests over a scripted NodeView tree. Build.VERSION.SDK_INT defaults to
// 0 under plain unit tests, so the O+ branches (enabled-wait / post-click refresh) are not
// exercised here - these cover the finder + the bounded perform-click retry (the R4 fix).
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultClearScenarioTest {

    private fun scenario() = DefaultClearScenario().apply {
        forceStopApps = false
        // MIN_WAIT_APP_PERFORM_CLICK_MS (2000) -> maxPerformClickCountTries == 7.
        maxWaitAppTimeoutMs = 2000
        arrayTextClearCacheButton.add("clear cache")
    }

    private fun clearCacheButton(clickResult: Boolean) =
        FakeNodeView(
            className = "android.widget.Button",
            text = "Clear cache",
            isClickable = true,
            enabledState = true,
            clickResult = clickResult,
        )

    @Test
    fun `clicks clear cache button and finishes`() = runTest {
        val button = clearCacheButton(clickResult = true)
        val root = FakeNodeView().child(button)

        val result = scenario().doClearCache(root)

        assertEquals(PACKAGE_FINISH.message, result?.message)
        assertTrue("button should be clicked", button.clickCount >= 1)
    }

    @Test
    fun `bounded retry fails the package when click never succeeds`() = runTest {
        val button = clearCacheButton(clickResult = false)
        val root = FakeNodeView().child(button)

        val result = scenario().doClearCache(root)

        assertEquals(PACKAGE_FINISH_FAILED.message, result?.message)
    }

    @Test
    fun `no matching button leaves cache step with no decision`() = runTest {
        // No clear-cache text in the tree and no storage menu -> doClearCache returns null
        // (nothing matched), i.e. the scenario yields to the next event.
        val root = FakeNodeView().child(
            FakeNodeView(className = "android.widget.TextView", text = "Something else")
        )

        val result = scenario().doClearCache(root)

        assertEquals(null, result)
    }
}
