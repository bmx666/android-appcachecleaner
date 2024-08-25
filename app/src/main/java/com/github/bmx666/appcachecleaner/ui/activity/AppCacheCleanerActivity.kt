package com.github.bmx666.appcachecleaner.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.FileUtils
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_SYSTEM
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_USER
import com.github.bmx666.appcachecleaner.ui.compose.AppScreen
import com.github.bmx666.appcachecleaner.ui.viewmodel.CleanResultViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.FirstBootViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.LocaleViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.PermissionViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsCustomPackageListViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsFilterViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsScenarioViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsTimeoutViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsUiViewModel
import com.github.bmx666.appcachecleaner.util.ActivityHelper
import com.github.bmx666.appcachecleaner.util.IIntentActivityCallback
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerActivityHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class AppCacheCleanerActivity : AppCompatActivity(), IIntentActivityCallback {

    private val firstBootViewModel: FirstBootViewModel by viewModels()
    private val settingsCustomPackageListViewModel: SettingsCustomPackageListViewModel by viewModels()
    private val settingsExtraViewModel: SettingsExtraViewModel by viewModels()
    private val settingsFilterViewModel: SettingsFilterViewModel by viewModels()
    private val settingsScenarioViewModel: SettingsScenarioViewModel by viewModels()
    private val settingsTimeoutViewModel: SettingsTimeoutViewModel by viewModels()
    private val settingsUiViewModel: SettingsUiViewModel by viewModels()

    private val localeViewModel: LocaleViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private val cleanResultViewModel: CleanResultViewModel by viewModels()

    private lateinit var localBroadcastManager: LocalBroadcastManagerActivityHelper

    private var calculationCleanedCacheJob: Job? = null
    private var loadingPkgListJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localBroadcastManager = LocalBroadcastManagerActivityHelper(this, this)

        observeNightMode()

        showSplashAndWaitSettings(
            isReady = {
                permissionViewModel.isReady.value
                && settingsCustomPackageListViewModel.isReady.value
                && settingsExtraViewModel.isReady.value
                && settingsFilterViewModel.isReady.value
                && settingsScenarioViewModel.isReady.value
                && settingsTimeoutViewModel.isReady.value
                && settingsUiViewModel.isReady.value
                && firstBootViewModel.isReady.value
                && localeViewModel.isReady.value
            })
    }

    private fun showSplashAndWaitSettings(isReady: () -> Boolean) {
        val content: View = findViewById(android.R.id.content)

        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isReady()) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        setContent {
                            CheckActions()
                            AppScreen(
                                localBroadcastManager,
                                localeViewModel,
                                cleanResultViewModel)
                        }
                        true
                    } else {
                        false
                    }
                }
            }
        )
    }

    private fun observeNightMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsUiViewModel.forceNightMode.collect { value ->
                    value?.let {
                        applyNightMode(value)
                    }
                }
            }
        }
    }

    private fun applyNightMode(isNightMode: Boolean) {
        val nightMode = if (isNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onDestroy() {
        loadingPkgListJob?.cancel()
        calculationCleanedCacheJob?.cancel()
        localBroadcastManager.onDestroy()
        super.onDestroy()
    }

    private val requestSaveLogFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != RESULT_OK) return@registerForActivityResult

        activityResult.data?.data?.let { uri ->
            contentResolver.openOutputStream(uri)?.let { outputStream ->
                try {
                    val inputStream = File(cacheDir.absolutePath + "/log.txt").inputStream()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        FileUtils.copy(inputStream, outputStream)
                    } else {
                        val buffer = ByteArray(8192)
                        var t: Int
                        while (inputStream.read(buffer).also { t = it } != -1)
                            outputStream.write(buffer, 0, t)
                    }
                    outputStream.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveLogFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "appcachecleaner-log.txt")
        }
        requestSaveLogFileLauncher.launch(intent)
    }

    @UiContext
    @UiThread
    override fun onClearCacheFinish(message: String?) {
        val interrupted: Boolean =
            when (message) {
                CANCEL_INTERRUPTED_BY_USER.message -> true
                CANCEL_INTERRUPTED_BY_SYSTEM.message -> true
                else -> false
            }

        cleanResultViewModel.finishClearCache(this, interrupted)

        // return back to Main Activity, sometimes not possible press Back from Settings
        ActivityHelper.returnBackToMainActivity(this, this.intent)

        if (BuildConfig.DEBUG)
            saveLogFile()

        if (message == CANCEL_INTERRUPTED_BY_SYSTEM.message)
            cleanResultViewModel.showInterruptedBySystemDialog()
    }

    @UiContext
    @UiThread
    override fun onClearDataFinish(message: String?) {
        val interrupted: Boolean =
            when (message) {
                CANCEL_INTERRUPTED_BY_USER.message -> true
                CANCEL_INTERRUPTED_BY_SYSTEM.message -> true
                else -> false
            }

        cleanResultViewModel.finishClearData(this, interrupted)

        // return back to Main Activity, sometimes not possible press Back from Settings
        ActivityHelper.returnBackToMainActivity(this, this.intent)

        if (BuildConfig.DEBUG)
            saveLogFile()
    }

    override fun onStopAccessibilityServiceFeedback() {
        permissionViewModel.checkAccessibilityPermission()
    }

    @Composable
    private fun CheckActions() {
        val actions by cleanResultViewModel.actions.collectAsState()
        val actionStopService by settingsExtraViewModel.actionStopService.collectAsState()
        val actionCloseApp by settingsExtraViewModel.actionCloseApp.collectAsState()

        if (actions) {
            if (actionStopService == true)
                localBroadcastManager.disableAccessibilityService()
            if (actionCloseApp == true)
                finish()
            cleanResultViewModel.resetActions()
        }
    }
}