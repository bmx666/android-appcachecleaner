package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.data.PackageRepository
import com.github.bmx666.appcachecleaner.fake.FakePackageSource
import com.github.bmx666.appcachecleaner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

// The final result screen: after a run the main screen shows a title ("...finished" /
// "...interrupted") and, on O+, the amount of cache cleaned. The amount can legitimately be
// 0 (cache already empty, or nothing actually dropped) - that is a normal result, not a
// failure. This covers the title transitions and the cleaned-bytes computation (incl. 0).
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class CleanResultViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun pkg(name: String) = PackageInfo().apply { packageName = name }

    @Test
    fun `successful finish shows the finished title and enables follow-up actions`() =
        runTest(mainRule.dispatcher) {
            val vm = CleanResultViewModel(PackageRepository(FakePackageSource()), context)

            vm.finishClearCache(interrupted = false)

            // synchronous title before the async size recompute overwrites it
            assertEquals(
                context.getString(R.string.text_clean_cache_finish_processing),
                vm.titleText
            )
            assertTrue("success should trigger post-run actions", vm.actions.value)
        }

    @Test
    fun `interrupted finish shows the interrupted title and skips actions`() =
        runTest(mainRule.dispatcher) {
            val vm = CleanResultViewModel(PackageRepository(FakePackageSource()), context)

            vm.finishClearCache(interrupted = true)

            assertEquals(
                context.getString(R.string.text_clean_cache_interrupt_processing),
                vm.titleText
            )
            assertFalse("interrupted run must not trigger actions", vm.actions.value)
        }

    @Test
    fun `cleaned bytes reflect the drop of checked packages`() = runTest(mainRule.dispatcher) {
        val source = FakePackageSource()
        val repo = PackageRepository(source)
        // loaded with 1000 bytes of cache, checked for cleaning
        repo.add(pkg("com.alpha"), "Alpha", Locale.US, 1000L)
        repo.setChecked("com.alpha", true)
        repo.applySortByLabel()

        // after the clean, the package reports 0 bytes -> 1000 cleaned
        source.cacheBytes["com.alpha"] = 0L
        assertEquals(1000L, repo.refreshStatsAfterCacheClean())
    }

    @Test
    fun `cleaned bytes are zero when nothing dropped - no panic`() = runTest(mainRule.dispatcher) {
        val source = FakePackageSource()
        val repo = PackageRepository(source)
        repo.add(pkg("com.alpha"), "Alpha", Locale.US, 1000L)
        repo.setChecked("com.alpha", true)
        repo.applySortByLabel()

        // unchanged size (e.g. cache could not be cleared) -> 0 cleaned, a valid result
        source.cacheBytes["com.alpha"] = 1000L
        assertEquals(0L, repo.refreshStatsAfterCacheClean())
    }
}
