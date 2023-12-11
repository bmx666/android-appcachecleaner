package com.github.bmx666.appcachecleaner.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.clearcache.AccessibilityClearCacheManager
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.ui.view.AccessibilityOverlay
import com.github.bmx666.appcachecleaner.util.IIntentServiceCallback
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerServiceHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppCacheCleanerService : AccessibilityService(), IIntentServiceCallback {

    companion object {
        private val accessibilityClearCacheManager = AccessibilityClearCacheManager()
    }

    private val logger = Logger()

    private lateinit var accessibilityOverlay: AccessibilityOverlay
    private lateinit var localBroadcastManager: LocalBroadcastManagerServiceHelper

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG)
            logger.onCreate(cacheDir)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        localBroadcastManager = LocalBroadcastManagerServiceHelper(this, this)
        accessibilityOverlay = AccessibilityOverlay {
            accessibilityClearCacheManager.interruptByUser()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        accessibilityOverlay.hide(this)
        accessibilityClearCacheManager.interruptBySystem()
        localBroadcastManager.onDestroy()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG)
            logger.onDestroy()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            accessibilityClearCacheManager.checkEvent(event)
    }

    override fun onInterrupt() {
    }

    override fun onStopAccessibilityService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        disableSelf()
    }

    override fun onClearCache(pkgList: ArrayList<String>?) {
        if (BuildConfig.DEBUG)
            logger.onClearCache()

        pkgList?.let{
            accessibilityOverlay.show(this)
            val pkgListSize = pkgList.size
            serviceScope.launch {
                val context = this@AppCacheCleanerService.applicationContext
                accessibilityClearCacheManager.setSettings(context)
                accessibilityClearCacheManager.clearCacheApp(
                    pkgList,
                    { index: Int ->
                        accessibilityOverlay.updateCounter(index, pkgListSize)
                    },
                    {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    },
                    localBroadcastManager::sendAppInfo,
                    localBroadcastManager::sendFinish)
            }
        } ?: localBroadcastManager.sendFinish(null, null)
    }

    override fun onCleanCacheFinish() {
        accessibilityOverlay.hide(this)
    }
}
