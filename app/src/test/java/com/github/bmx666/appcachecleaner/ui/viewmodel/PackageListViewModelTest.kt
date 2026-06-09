package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.github.bmx666.appcachecleaner.data.LocaleManager
import com.github.bmx666.appcachecleaner.data.PackageRepository
import com.github.bmx666.appcachecleaner.data.UserPrefCustomPackageListManager
import com.github.bmx666.appcachecleaner.data.UserPrefFilterManager
import com.github.bmx666.appcachecleaner.fake.FakePackageSource
import com.github.bmx666.appcachecleaner.fake.TestDispatcherProvider
import com.github.bmx666.appcachecleaner.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

// Verifies the ViewModel delegates selection to the repository (which is the single source
// of truth). Managers are MockK'd (final classes); the repo + PackageSource are real fakes.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class PackageListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun viewModel(repo: PackageRepository) = PackageListViewModel(
        userPrefCustomPackageListManager = mockk<UserPrefCustomPackageListManager>(relaxed = true),
        userPrefFilterManager = mockk<UserPrefFilterManager>(relaxed = true),
        localeManager = mockk<LocaleManager>(relaxed = true),
        repo = repo,
        packageSource = FakePackageSource(),
        dispatchers = TestDispatcherProvider(mainRule.dispatcher),
        context = context,
    )

    @Test
    fun `checkPackage toggles selection in the repository`() = runTest(mainRule.dispatcher) {
        val repo = PackageRepository(FakePackageSource())
        repo.add(PackageInfo().apply { packageName = "com.a" }, "A", Locale.US, 0)
        repo.applySortByLabel()

        val vm = viewModel(repo)

        vm.checkPackage("com.a", true)
        advanceUntilIdle()
        assertEquals(setOf("com.a"), repo.checked.value)

        vm.checkPackage("com.a", false)
        advanceUntilIdle()
        assertEquals(emptySet<String>(), repo.checked.value)
    }
}
