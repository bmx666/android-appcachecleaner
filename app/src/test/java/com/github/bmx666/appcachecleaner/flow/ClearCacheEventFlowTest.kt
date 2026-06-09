package com.github.bmx666.appcachecleaner.flow

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_USER
import com.github.bmx666.appcachecleaner.data.PackageRepository
import com.github.bmx666.appcachecleaner.fake.FakePackageSource
import com.github.bmx666.appcachecleaner.util.DefaultEventBus
import com.github.bmx666.appcachecleaner.util.IIntentActivityCallback
import com.github.bmx666.appcachecleaner.util.IIntentServiceCallback
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerActivityHelper
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerServiceHelper
import com.github.bmx666.appcachecleaner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

// "Select a few apps, run clean, see the result" at the messaging boundary: the activity
// builds the checked-package list from the repository and broadcasts it over the EventBus;
// the accessibility service receives exactly that list; when the service reports the run is
// finished (or interrupted), the activity gets the message back for the main screen.
//
// Real DefaultEventBus + real LocalBroadcastManager helpers (the LocalBroadcastManager
// replacement) + real PackageRepository. Collectors live on Dispatchers.Main.immediate, so
// the test swaps Main for a test dispatcher and advances it; replay = 0 means we must let
// the collectors subscribe (advanceUntilIdle) before emitting.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ClearCacheEventFlowTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private class ServiceRecorder : IIntentServiceCallback {
        var clearCacheList: ArrayList<String>? = null
        var clearCacheFinished = false
        var clearDataList: ArrayList<String>? = null
        override fun onStopAccessibilityService() {}
        override fun onClearCache(pkgList: ArrayList<String>?) { clearCacheList = pkgList }
        override fun onClearCacheFinish() { clearCacheFinished = true }
        override fun onClearData(pkgList: ArrayList<String>?) { clearDataList = pkgList }
        override fun onClearDataFinish() {}
    }

    private class ActivityRecorder : IIntentActivityCallback {
        var cacheFinishCalls = 0
        var cacheFinishMessage: String? = "UNSET"
        override fun onClearCacheFinish(message: String?) {
            cacheFinishCalls++; cacheFinishMessage = message
        }
        override fun onClearDataFinish(message: String?) {}
        override fun onStopAccessibilityServiceFeedback() {}
    }

    private fun pkg(name: String) = PackageInfo().apply { packageName = name }

    @Test
    fun `checked selection is delivered to the service and finish round-trips back`() =
        runTest(mainRule.dispatcher) {
            // ---- user selects a few apps in the list ----
            val repo = PackageRepository(FakePackageSource())
            repo.add(pkg("com.alpha"), "Alpha", Locale.US, 0)
            repo.add(pkg("com.beta"), "Beta", Locale.US, 0)
            repo.add(pkg("com.gamma"), "Gamma", Locale.US, 0)
            repo.applySortByLabel()
            repo.setChecked("com.alpha", true)
            repo.setChecked("com.gamma", true)

            val selection = repo.getChecked()
            assertEquals(setOf("com.alpha", "com.gamma"), selection)

            // ---- wire the real broadcast helpers over one EventBus ----
            val eventBus = DefaultEventBus()
            val service = ServiceRecorder()
            val activity = ActivityRecorder()
            val serviceHelper = LocalBroadcastManagerServiceHelper(context, eventBus, service)
            val activityHelper = LocalBroadcastManagerActivityHelper(context, eventBus, activity)
            advanceUntilIdle() // let both collectors subscribe (replay = 0)

            // ---- run clean: activity broadcasts the selected list ----
            activityHelper.sendPackageListToClearCache(ArrayList(selection))
            advanceUntilIdle()

            assertEquals(
                "service must receive exactly the selected packages",
                selection, service.clearCacheList?.toSet()
            )

            // ---- service reports success -> activity shows the finished result ----
            serviceHelper.sendFinishClearCache(null, null)
            advanceUntilIdle()

            assertTrue(service.clearCacheFinished)
            assertEquals(1, activity.cacheFinishCalls)
            assertNull("null message -> success on the main screen", activity.cacheFinishMessage)

            serviceHelper.onDestroy()
            activityHelper.onDestroy()
        }

    @Test
    fun `interrupt finish carries the interrupt message to the activity`() =
        runTest(mainRule.dispatcher) {
            val eventBus = DefaultEventBus()
            val activity = ActivityRecorder()
            val activityHelper = LocalBroadcastManagerActivityHelper(context, eventBus, activity)
            val serviceHelper =
                LocalBroadcastManagerServiceHelper(context, eventBus, ServiceRecorder())
            advanceUntilIdle()

            // overlay-stop -> manager finishes with the interrupt message -> service broadcasts it
            serviceHelper.sendFinishClearCache(CANCEL_INTERRUPTED_BY_USER.message, "com.alpha")
            advanceUntilIdle()

            assertEquals(
                CANCEL_INTERRUPTED_BY_USER.message,
                activity.cacheFinishMessage
            )

            serviceHelper.onDestroy()
            activityHelper.onDestroy()
        }
}
