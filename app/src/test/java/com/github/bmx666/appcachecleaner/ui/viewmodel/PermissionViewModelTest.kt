package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.github.bmx666.appcachecleaner.service.AppCacheCleanerService
import com.github.bmx666.appcachecleaner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Permission gating that decides whether the main screen can start a run. The accessibility
// state is read from Settings.Secure (shadowed here); usage-stats from AppOps (Robolectric
// defaults to MODE_ALLOWED). Flows are stateIn(Lazily), so we collect (first) rather than
// read .value.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class PermissionViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun enableAccessibilityService() {
        Settings.Secure.putInt(
            context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        val serviceName =
            context.packageName + "/" + AppCacheCleanerService::class.java.name
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceName)
    }

    @Test
    fun `reports granted when accessibility is enabled and usage-stats allowed`() =
        runTest(mainRule.dispatcher) {
            enableAccessibilityService()

            val vm = PermissionViewModel(context)
            advanceUntilIdle()

            assertEquals(true, vm.hasAccessibilityPermission.first { it != null })
            assertEquals(true, vm.hasUsageStatsPermission.first { it != null })
            assertEquals(true, vm.isReady.first { it })
        }

    @Test
    fun `reports accessibility not granted when the service is not enabled`() =
        runTest(mainRule.dispatcher) {
            // Real devices always have ACCESSIBILITY_ENABLED present (default 0); set it so
            // the prod check takes the `!= 1` branch instead of Robolectric's unset-setting
            // SettingNotFoundException path (which prod catches, but it spams stderr).
            Settings.Secure.putInt(
                context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
            val vm = PermissionViewModel(context)
            advanceUntilIdle()

            assertEquals(false, vm.hasAccessibilityPermission.first { it != null })
        }
}
