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

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                mLocalReceiver,
                IntentFilter().apply {
                    addAction(Constant.Intent.StopAccessibilityService.ACTION)
                    addAction(Constant.Intent.ExtraSearchText.ACTION)
                    addAction(Constant.Intent.ClearCache.ACTION)
                }
            )

        accessibilityOverlay = AccessibilityOverlay(this) {
            accessibilityClearCacheManager.interrupt()
        }
    }

    private fun updateLocaleText(clearCacheText: CharSequence?, storageText: CharSequence?) {
        accessibilityClearCacheManager.apply {
            setArrayTextClearCacheButton(
                ArrayList<CharSequence>().apply {
                    clearCacheText?.let { add(it) }
                    add(getText(R.string.clear_cache_btn_text))
                }
            )

            setArrayTextStorageAndCacheMenu(
                ArrayList<CharSequence>().apply {
                    storageText?.let { add(it) }
                    add(getText(R.string.storage_settings_for_app))
                    add(getText(R.string.storage_label))
                }
            )
        }
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
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(Constant.Intent.CleanCacheAppInfo.ACTION).apply {
                putExtra(Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME, pkgName)
            }
        )
    }

    private suspend fun clearCache(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG)
            logger.onClearCache()

        Handler(Looper.getMainLooper()).post {
            accessibilityOverlay.show()
        }

        val interrupted = accessibilityClearCacheManager.clearCacheApp(pkgList, this::openAppInfo)
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(Constant.Intent.CleanCacheFinish.ACTION).apply {
                putExtra(Constant.Intent.CleanCacheFinish.NAME_INTERRUPTED, interrupted)
            }
        )

        Handler(Looper.getMainLooper()).post {
            accessibilityOverlay.hide()
        }
    }

    companion object {
        private val accessibilityClearCacheManager = AccessibilityClearCacheManager()
    }
}