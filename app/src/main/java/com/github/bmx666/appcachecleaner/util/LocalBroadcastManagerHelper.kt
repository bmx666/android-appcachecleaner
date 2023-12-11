package com.github.bmx666.appcachecleaner.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.service.AppInfoService


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
    fun onCleanCacheFinish(message: String?)
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
                        Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME
                    )
                    if (BuildConfig.DEBUG)
                        Logger.d("[Activity] CleanCacheAppInfo: package name = $pkgName")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val serviceIntent = Intent(
                            this@LocalBroadcastManagerActivityHelper.context,
                            AppInfoService::class.java).apply {
                            putExtra(
                                Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME,
                                pkgName)
                        }
                        ContextCompat.startForegroundService(
                            this@LocalBroadcastManagerActivityHelper.context,
                            serviceIntent)
                    } else {
                        ActivityHelper.startApplicationDetailsActivity(
                            this@LocalBroadcastManagerActivityHelper.context,
                            pkgName
                        )
                    }
                }
                Constant.Intent.CleanCacheFinish.ACTION -> {
                    val message = intent.getStringExtra(
                        Constant.Intent.CleanCacheFinish.NAME_MESSAGE)
                    val pkgName = intent.getStringExtra(
                        Constant.Intent.CleanCacheFinish.NAME_PACKAGE_NAME)
                    if (BuildConfig.DEBUG)
                        Logger.d("[Activity] CleanCacheFinish: message = $message, pkgName = $pkgName")
                    callback.onCleanCacheFinish(message)
                }
                Constant.Intent.StopAccessibilityServiceFeedback.ACTION -> {
                    if (BuildConfig.DEBUG)
                        Logger.d("[Activity] StopAccessibilityServiceFeedback")
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
        if (BuildConfig.DEBUG)
            Logger.d("[Activity] disableAccessibilityService")
        // Android 6 doesn't have methods to disable Accessibility service
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
            ActivityHelper.showAccessibilitySettings(context)
        else
            sendBroadcast(Intent(Constant.Intent.StopAccessibilityService.ACTION))
    }

    fun sendPackageList(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG) {
            Logger.d("[Activity] sendPackageList")
            pkgList.forEach {
                Logger.d("[Activity] sendPackageList: package name = $it")
            }
        }
        sendBroadcast(
            Intent(Constant.Intent.ClearCache.ACTION).apply {
                putStringArrayListExtra(Constant.Intent.ClearCache.NAME_PACKAGE_LIST, pkgList)
            }
        )
    }
}

interface IIntentServiceCallback {
    fun onStopAccessibilityService()
    fun onClearCache(pkgList: ArrayList<String>?)
    fun onCleanCacheFinish()
}

data class IntentSettings(
    val clearCacheTextList: Array<String>?,
    val clearDataTextList: Array<String>?,
    val storageTextList: Array<String>?,
    val okTextList: Array<String>?,
    val scenario: Constant.Scenario?,
    val delayForNextAppTimeout: Int?,
    val maxWaitAppTimeout: Int?,
    val maxWaitClearCacheButtonTimeout: Int?,
    val maxWaitAccessibilityEventTimeout: Int?,
    val goBackAfterApps: Int?,
)

class LocalBroadcastManagerServiceHelper(
    context: Context,
    callback: IIntentServiceCallback): BaseLocalBroadcastManagerHelper(context) {

    override val localBroadcastReceiver = object : Callback() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.Intent.StopAccessibilityService.ACTION -> {
                    if (BuildConfig.DEBUG)
                        Logger.d("[Service] StopAccessibilityService")
                    // send back to Activity - service was stopped
                    sendBroadcast(Intent(Constant.Intent.StopAccessibilityServiceFeedback.ACTION))
                    callback.onStopAccessibilityService()
                }
                Constant.Intent.ClearCache.ACTION -> {
                    val pkgList =
                        intent.getStringArrayListExtra(Constant.Intent.ClearCache.NAME_PACKAGE_LIST)
                    if (BuildConfig.DEBUG) {
                        Logger.d("[Service] ClearCache")
                        pkgList?.forEach {
                            Logger.d("[Service] ClearCache: package name = $it")
                        }
                    }
                    callback.onClearCache(pkgList)
                }
                Constant.Intent.CleanCacheFinish.ACTION -> {
                    if (BuildConfig.DEBUG)
                        Logger.d("[Service] CleanCacheFinish")
                    callback.onCleanCacheFinish()
                }
            }
        }
    }

    override val intentFilter = IntentFilter().apply {
        addAction(Constant.Intent.StopAccessibilityService.ACTION)
        addAction(Constant.Intent.ClearCache.ACTION)
        addAction(Constant.Intent.CleanCacheFinish.ACTION)
    }

    init {
        register()
    }

    fun sendAppInfo(pkgName: String) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendAppInfo: package name = $pkgName")
        sendBroadcast(
            Intent(Constant.Intent.CleanCacheAppInfo.ACTION).apply {
                putExtra(Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME, pkgName)
            }
        )
    }

    fun sendFinish(message: String?,
                   pkgName: String?) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendFinish: message = $message, pkgName = $pkgName")
        sendBroadcast(
            Intent(Constant.Intent.CleanCacheFinish.ACTION).apply {
                putExtra(
                    Constant.Intent.CleanCacheFinish.NAME_MESSAGE,
                    message)
                putExtra(
                    Constant.Intent.CleanCacheFinish.NAME_PACKAGE_NAME,
                    pkgName)
            }
        )
    }
}
