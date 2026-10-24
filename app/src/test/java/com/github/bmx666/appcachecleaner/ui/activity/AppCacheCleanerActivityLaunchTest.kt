package com.github.bmx666.appcachecleaner.ui.activity

import android.os.Build
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import com.github.bmx666.appcachecleaner.ui.viewmodel.FirstBootViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.LocaleViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.PermissionViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsCustomPackageListViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsFilterViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsScenarioViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsTimeoutViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsUiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

// Launch regression guard: the activity holds the first frame (pre-draw listener) until every
// settings ViewModel is ready, then calls setContent -> NavHost. NavHost requires a
// NavigationEventDispatcherOwner on the view tree, which AppCompatActivity.setContentView does not
// register; a missing owner throws during the first composition. Runs on the real Hilt graph from
// the manifest Application. DataStore loads on real IO threads, so the main looper is advanced
// frame by frame until the ComposeView appears (or the deadline expires).
//
// Robolectric does not reliably re-run a traversal after onPreDraw cancels the draw, so the
// pre-draw listener is dispatched explicitly each iteration; composition errors thrown from
// setContent propagate out of onActivity and fail the test with the real exception.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AppCacheCleanerActivityLaunchTest {

    private companion object {
        const val READY_TIMEOUT_MS = 10_000L
        val FRAME: Duration = Duration.ofMillis(16)
    }

    @Test
    fun `composes NavHost after splash without crashing`() {
        ActivityScenario.launch(AppCacheCleanerActivity::class.java).use { scenario ->
            var composed = false
            val deadline = System.currentTimeMillis() + READY_TIMEOUT_MS
            while (!composed && System.currentTimeMillis() < deadline) {
                shadowOf(Looper.getMainLooper()).idleFor(FRAME)
                scenario.onActivity { activity ->
                    val content = activity.findViewById<ViewGroup>(android.R.id.content)
                    content.viewTreeObserver.dispatchOnPreDraw()
                    composed = content.childCount > 0 && content.getChildAt(0) is ComposeView
                }
                if (!composed) Thread.sleep(10)
            }

            var notReady = emptyList<String>()
            if (!composed) scenario.onActivity { activity -> notReady = notReadyViewModels(activity) }

            assertTrue(
                "splash never released: setContent was not reached; not ready: $notReady",
                composed)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    // Same instances the activity holds via `by viewModels()` (same store, same default key).
    private fun notReadyViewModels(activity: AppCacheCleanerActivity): List<String> {
        val provider = ViewModelProvider(activity)
        return listOf(
            "PermissionViewModel" to provider[PermissionViewModel::class.java].isReady.value,
            "SettingsCustomPackageListViewModel" to
                provider[SettingsCustomPackageListViewModel::class.java].isReady.value,
            "SettingsExtraViewModel" to provider[SettingsExtraViewModel::class.java].isReady.value,
            "SettingsFilterViewModel" to provider[SettingsFilterViewModel::class.java].isReady.value,
            "SettingsScenarioViewModel" to
                provider[SettingsScenarioViewModel::class.java].isReady.value,
            "SettingsTimeoutViewModel" to
                provider[SettingsTimeoutViewModel::class.java].isReady.value,
            "SettingsUiViewModel" to provider[SettingsUiViewModel::class.java].isReady.value,
            "FirstBootViewModel" to provider[FirstBootViewModel::class.java].isReady.value,
            "LocaleViewModel" to provider[LocaleViewModel::class.java].isReady.value,
        ).filterNot { it.second }.map { it.first }
    }
}
