package com.github.bmx666.appcachecleaner.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.ui.view.AccessibilityOverlay
import com.github.bmx666.appcachecleaner.util.AccessibilityClearCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppCacheCleanerService : AccessibilityService() {

    private val logger = Logger()

    private lateinit var accessibilityOverlay: AccessibilityOverlay

    private val mLocalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.Intent.StopAccessibilityService.ACTION -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
                    // send back to Activity - service was stopped
                    LocalBroadcastManager.getInstance(this@AppCacheCleanerService)
                        .sendBroadcast(Intent(Constant.Intent.StopAccessibilityServiceFeedback.ACTION))
                    disableSelf()
                }
                Constant.Intent.ExtraSearchText.ACTION -> {
                    updateLocaleText(
                        intent.getStringExtra(Constant.Intent.ExtraSearchText.NAME_CLEAR_CACHE),
                        intent.getStringExtra(Constant.Intent.ExtraSearchText.NAME_STORAGE)
                    )
                }
                Constant.Intent.ClearCache.ACTION -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        clearCache(
                            intent.getStringArrayListExtra(Constant.Intent.ClearCache.NAME_PACKAGE_LIST)!!
                        )
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG)
            logger.onCreate(cacheDir)

        updateLocaleText(null, null)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constant.Intent.StopAccessibilityService.ACTION)
        intentFilter.addAction(Constant.Intent.ExtraSearchText.ACTION)
        intentFilter.addAction(Constant.Intent.ClearCache.ACTION)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mLocalReceiver, intentFilter)

        accessibilityOverlay = AccessibilityOverlay(this) {
            accessibilityClearCacheManager.interrupt()
        }
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
            logger.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            accessibilityClearCacheManager.checkEvent(event)
    }

    override fun onInterrupt() {
    }

    private fun openAppInfo(pkgName: String) {
        val intent = Intent(Constant.Intent.CleanCacheAppInfo.ACTION)
        intent.putExtra(Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME, pkgName)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private suspend fun clearCache(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG)
            logger.onClearCache()

        Handler(Looper.getMainLooper()).post {
            accessibilityOverlay.show()
        }

        val interrupted = accessibilityClearCacheManager.clearCacheApp(pkgList, this::openAppInfo)
        val intent = Intent(Constant.Intent.CleanCacheFinish.ACTION)
        intent.putExtra(Constant.Intent.CleanCacheFinish.NAME_INTERRUPTED, interrupted)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        Handler(Looper.getMainLooper()).post {
            accessibilityOverlay.hide()
        }
    }

    companion object {
        private val accessibilityClearCacheManager = AccessibilityClearCacheManager()
    }
}