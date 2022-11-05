package com.github.bmx666.appcachecleaner

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.log.TimberFileTree
import com.github.bmx666.appcachecleaner.util.AccessibilityClearCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class AppCacheCleanerService : AccessibilityService() {

    private var mAccessibilityButtonController: AccessibilityButtonController? = null
    private var mAccessibilityButtonCallback: AccessibilityButtonCallback? = null
    private var mIsAccessibilityButtonAvailable: Boolean = false

    private val mLocalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.Intent.DisableSelf.ACTION -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) unregisterButton()
                    disableSelf()
                }
                Constant.Intent.ExtraSearchText.ACTION -> {
                    updateLocaleText(
                        intent.getStringExtra(Constant.Intent.ExtraSearchText.NAME_CLEAR_CACHE),
                        intent.getStringExtra(Constant.Intent.ExtraSearchText.NAME_STORAGE))
                }
                Constant.Intent.ClearCache.ACTION -> {
                    CoroutineScope(IO).launch {
                        clearCache(
                            intent.getStringArrayListExtra(Constant.Intent.ClearCache.NAME_PACKAGE_LIST)!!)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            cleanLogFile()
            Timber.plant(TimberFileTree(getLogFile()))
        }
        updateLocaleText(null, null)
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constant.Intent.DisableSelf.ACTION)
        intentFilter.addAction(Constant.Intent.ExtraSearchText.ACTION)
        intentFilter.addAction(Constant.Intent.ClearCache.ACTION)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mLocalReceiver, intentFilter)
    }

    private fun updateLocaleText(clearCacheText: CharSequence?, storageText: CharSequence?) {
        val arrayTextClearCacheButton = ArrayList<CharSequence>()
        clearCacheText?.let { arrayTextClearCacheButton.add(it) }
        arrayTextClearCacheButton.add(getText(R.string.clear_cache_btn_text))
        accessibilityClearCacheManager.setArrayTextClearCacheButton(arrayTextClearCacheButton)

        val arrayTextStorageAndCacheMenu = ArrayList<CharSequence>()
        storageText?.let { arrayTextStorageAndCacheMenu.add(it) }
        arrayTextStorageAndCacheMenu.add(getText(R.string.storage_settings_for_app))
        arrayTextStorageAndCacheMenu.add(getText(R.string.storage_label))
        accessibilityClearCacheManager.setArrayTextStorageAndCacheMenu(arrayTextStorageAndCacheMenu)
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG)
            deleteLogFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            unregisterButton()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver)
        super.onDestroy()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            registerButton()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerButton() {
        mAccessibilityButtonController = accessibilityButtonController

        // Accessibility Button is available on Android 30 and early
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (mAccessibilityButtonController?.isAccessibilityButtonAvailable != true)
                return
        }

        mAccessibilityButtonCallback =
            object : AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    accessibilityClearCacheManager.clickAccessibilityButton()
                }

                override fun onAvailabilityChanged(
                    controller: AccessibilityButtonController,
                    available: Boolean
                ) {
                    if (controller == mAccessibilityButtonController) {
                        Timber.d("Accessibility button available = $available")
                        mIsAccessibilityButtonAvailable = available
                    }
                }
            }

        mAccessibilityButtonCallback?.also {
            mAccessibilityButtonController?.registerAccessibilityButtonCallback(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun unregisterButton() {
        mAccessibilityButtonCallback?.let {
            mAccessibilityButtonController?.unregisterAccessibilityButtonCallback(it)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return

        accessibilityClearCacheManager.checkEvent(event)
    }

    override fun onInterrupt() {}

    private fun getLogFile(): File {
        return File(cacheDir.absolutePath + "/log.txt")
    }

    private fun cleanLogFile() {
        // force clean previous log
        getLogFile().writeText("")
    }

    private fun deleteLogFile() {
        getLogFile().delete()
    }

    private fun openAppInfo(pkgName: String) {
        val intent = Intent(Constant.Intent.CleanCacheAppInfo.ACTION)
        intent.putExtra(Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME, pkgName)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private suspend fun clearCache(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG)
            cleanLogFile()
        val interrupted = accessibilityClearCacheManager.clearCacheApp(pkgList, this::openAppInfo)
        val intent = Intent(Constant.Intent.CleanCacheFinish.ACTION)
        intent.putExtra(Constant.Intent.CleanCacheFinish.NAME_INTERRUPTED, interrupted)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        private val accessibilityClearCacheManager = AccessibilityClearCacheManager()
    }
}
