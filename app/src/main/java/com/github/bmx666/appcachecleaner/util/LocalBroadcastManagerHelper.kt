package com.github.bmx666.appcachecleaner.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.const.Constant

abstract class BaseLocalBroadcastManagerHelper(protected val context: Context) {

    abstract class Callback {
        open fun onReceive(context: Context?, intent: Intent?) {}
    }

    private val internalBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            localBroadcastReceiver.onReceive(context, intent)
        }
    }

    private val localBroadcastManager = LocalBroadcastManager.getInstance(context)
    protected abstract val localBroadcastReceiver: Callback
    protected abstract val intentFilter: IntentFilter

    protected fun register() {
        localBroadcastManager.registerReceiver(internalBroadcastReceiver, intentFilter)
    }

    fun onDestroy() {
        localBroadcastManager.unregisterReceiver(internalBroadcastReceiver)
    }

    fun sendBroadcast(intent: Intent) {
        localBroadcastManager.sendBroadcast(intent)
    }
}

interface IIntentActivityCallback {
    fun onCleanCacheFinish(interrupted: Boolean)
    fun onStopAccessibilityServiceFeedback()
}

class LocalBroadcastManagerActivityHelper(
    context: Context,
    callback: IIntentActivityCallback): BaseLocalBroadcastManagerHelper(context) {

    override val localBroadcastReceiver = object : Callback() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.Intent.CleanCacheAppInfo.ACTION -> {
                    val pkgName = intent.getStringExtra(
                        Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME)
                    ActivityHelper.startApplicationDetailsActivity(
                        this@LocalBroadcastManagerActivityHelper.context,
                        pkgName
                    )
                }
                Constant.Intent.CleanCacheFinish.ACTION -> {
                    val interrupted = intent.getBooleanExtra(
                        Constant.Intent.CleanCacheFinish.NAME_INTERRUPTED,
                        false
                    )
                    callback.onCleanCacheFinish(interrupted)
                }
                Constant.Intent.StopAccessibilityServiceFeedback.ACTION -> {
                    callback.onStopAccessibilityServiceFeedback()
                }
            }
        }
    }

    override val intentFilter = IntentFilter().apply {
        addAction(Constant.Intent.CleanCacheAppInfo.ACTION)
        addAction(Constant.Intent.CleanCacheFinish.ACTION)
        addAction(Constant.Intent.StopAccessibilityServiceFeedback.ACTION)
    }

    init {
        register()
    }

    fun disableAccessibilityService() {
        // Android 6 doesn't have methods to disable Accessibility service
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
            ActivityHelper.showAccessibilitySettings(context)
        else
            sendBroadcast(Intent(Constant.Intent.StopAccessibilityService.ACTION))
    }

    fun sendPackageList(pkgList: ArrayList<String>) {
        sendBroadcast(
            Intent(Constant.Intent.ClearCache.ACTION).apply {
                putStringArrayListExtra(Constant.Intent.ClearCache.NAME_PACKAGE_LIST, pkgList)
            }
        )
    }
}

interface IIntentServiceCallback {
    fun onStopAccessibilityService()
    fun onExtraSearchText(clearCacheTextList: Array<String>?, storageTextList: Array<String>?)
    fun onClearCache(pkgList: ArrayList<String>?)
    fun onCleanCacheFinish()
}

class LocalBroadcastManagerServiceHelper(
    context: Context,
    callback: IIntentServiceCallback): BaseLocalBroadcastManagerHelper(context) {

    override val localBroadcastReceiver = object : Callback() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.Intent.StopAccessibilityService.ACTION -> {
                    // send back to Activity - service was stopped
                    sendBroadcast(Intent(Constant.Intent.StopAccessibilityServiceFeedback.ACTION))
                    callback.onStopAccessibilityService()
                }
                Constant.Intent.ExtraSearchText.ACTION -> {
                    callback.onExtraSearchText(
                        intent.getStringArrayExtra(Constant.Intent.ExtraSearchText.NAME_CLEAR_CACHE_TEXT_LIST),
                        intent.getStringArrayExtra(Constant.Intent.ExtraSearchText.NAME_STORAGE_TEXT_LIST)
                    )
                }
                Constant.Intent.ClearCache.ACTION -> {
                    callback.onClearCache(
                        intent.getStringArrayListExtra(Constant.Intent.ClearCache.NAME_PACKAGE_LIST))
                }
                Constant.Intent.CleanCacheFinish.ACTION -> {
                    callback.onCleanCacheFinish()
                }
            }
        }
    }

    override val intentFilter = IntentFilter().apply {
        addAction(Constant.Intent.StopAccessibilityService.ACTION)
        addAction(Constant.Intent.ExtraSearchText.ACTION)
        addAction(Constant.Intent.ClearCache.ACTION)
        addAction(Constant.Intent.CleanCacheFinish.ACTION)
    }

    init {
        register()
    }

    fun sendAppInfo(pkgName: String) {
        sendBroadcast(
            Intent(Constant.Intent.CleanCacheAppInfo.ACTION).apply {
                putExtra(Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME, pkgName)
            }
        )
    }

    fun sendFinish(interrupted: Boolean) {
        sendBroadcast(
            Intent(Constant.Intent.CleanCacheFinish.ACTION).apply {
                putExtra(Constant.Intent.CleanCacheFinish.NAME_INTERRUPTED, interrupted)
            }
        )
    }
}