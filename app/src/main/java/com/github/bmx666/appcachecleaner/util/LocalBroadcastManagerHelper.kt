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
    fun onClearCacheFinish(message: String?)
    fun onClearDataFinish(message: String?)
    fun onStopAccessibilityServiceFeedback()
}

class LocalBroadcastManagerActivityHelper(
    context: Context,
    callback: IIntentActivityCallback): BaseLocalBroadcastManagerHelper(context) {

    override val localBroadcastReceiver = object : Callback() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.Intent.AppInfo.ACTION -> {
                    val pkgName = intent.getStringExtra(
                        Constant.Intent.AppInfo.NAME_PACKAGE_NAME
                    )
                    if (BuildConfig.DEBUG)
                        Logger.d("[Activity] AppInfo: package name = $pkgName")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val serviceIntent = Intent(
                            this@LocalBroadcastManagerActivityHelper.context,
                            AppInfoService::class.java).apply {
                            putExtra(
                                Constant.Intent.AppInfo.NAME_PACKAGE_NAME,
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
                Constant.Intent.ClearCacheFinish.ACTION -> {
                    val message = intent.getStringExtra(
                        Constant.Intent.ClearCacheFinish.NAME_MESSAGE)
                    val pkgName = intent.getStringExtra(
                        Constant.Intent.ClearCacheFinish.NAME_PACKAGE_NAME)
                    if (BuildConfig.DEBUG)
                        Logger.d("[Activity] ClearCacheFinish: message = $message, pkgName = $pkgName")
                    callback.onClearCacheFinish(message)
                }
                Constant.Intent.ClearDataFinish.ACTION -> {
                    val message = intent.getStringExtra(
                        Constant.Intent.ClearDataFinish.NAME_MESSAGE)
                    val pkgName = intent.getStringExtra(
                        Constant.Intent.ClearDataFinish.NAME_PACKAGE_NAME)
                    if (BuildConfig.DEBUG)
                        Logger.d("[Activity] ClearDataFinish: message = $message, pkgName = $pkgName")
                    callback.onClearDataFinish(message)
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
        addAction(Constant.Intent.AppInfo.ACTION)
        addAction(Constant.Intent.ClearCacheFinish.ACTION)
        addAction(Constant.Intent.ClearDataFinish.ACTION)
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

    fun sendPackageListToClearCache(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG) {
            Logger.d("[Activity] sendPackageListToClearCache")
            pkgList.forEach {
                Logger.d("[Activity] sendPackageListToClearCache: package name = $it")
            }
        }
        sendBroadcast(
            Intent(Constant.Intent.ClearCache.ACTION).apply {
                putStringArrayListExtra(Constant.Intent.ClearCache.NAME_PACKAGE_LIST, pkgList)
            }
        )
    }

    fun sendPackageListToClearData(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG) {
            Logger.d("[Activity] sendPackageListToClearData")
            pkgList.forEach {
                Logger.d("[Activity] sendPackageListToClearData: package name = $it")
            }
        }
        sendBroadcast(
            Intent(Constant.Intent.ClearData.ACTION).apply {
                putStringArrayListExtra(Constant.Intent.ClearData.NAME_PACKAGE_LIST, pkgList)
            }
        )
    }
}

interface IIntentServiceCallback {
    fun onStopAccessibilityService()
    fun onClearCache(pkgList: ArrayList<String>?)
    fun onClearCacheFinish()
    fun onClearData(pkgList: ArrayList<String>?)
    fun onClearDataFinish()
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
                Constant.Intent.ClearCacheFinish.ACTION -> {
                    if (BuildConfig.DEBUG)
                        Logger.d("[Service] ClearCacheFinish")
                    callback.onClearCacheFinish()
                }
                Constant.Intent.ClearData.ACTION -> {
                    val pkgList =
                        intent.getStringArrayListExtra(Constant.Intent.ClearData.NAME_PACKAGE_LIST)
                    if (BuildConfig.DEBUG) {
                        Logger.d("[Service] ClearData")
                        pkgList?.forEach {
                            Logger.d("[Service] ClearData: package name = $it")
                        }
                    }
                    callback.onClearData(pkgList)
                }
                Constant.Intent.ClearDataFinish.ACTION -> {
                    if (BuildConfig.DEBUG)
                        Logger.d("[Service] CleanDataFinish")
                    callback.onClearDataFinish()
                }
            }
        }
    }

    override val intentFilter = IntentFilter().apply {
        addAction(Constant.Intent.StopAccessibilityService.ACTION)
        addAction(Constant.Intent.ClearCache.ACTION)
        addAction(Constant.Intent.ClearCacheFinish.ACTION)
        addAction(Constant.Intent.ClearData.ACTION)
        addAction(Constant.Intent.ClearDataFinish.ACTION)
    }

    init {
        register()
    }

    fun sendAppInfo(pkgName: String) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendAppInfo: package name = $pkgName")
        sendBroadcast(
            Intent(Constant.Intent.AppInfo.ACTION).apply {
                putExtra(Constant.Intent.AppInfo.NAME_PACKAGE_NAME, pkgName)
            }
        )
    }

    fun sendFinishClearCache(message: String?,
                   pkgName: String?) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendFinishClearCache: message = $message, pkgName = $pkgName")
        sendBroadcast(
            Intent(Constant.Intent.ClearCacheFinish.ACTION).apply {
                putExtra(
                    Constant.Intent.ClearCacheFinish.NAME_MESSAGE,
                    message)
                putExtra(
                    Constant.Intent.ClearCacheFinish.NAME_PACKAGE_NAME,
                    pkgName)
            }
        )
    }

    fun sendFinishClearData(message: String?,
                             pkgName: String?) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendFinishClearData: message = $message, pkgName = $pkgName")
        sendBroadcast(
            Intent(Constant.Intent.ClearDataFinish.ACTION).apply {
                putExtra(
                    Constant.Intent.ClearDataFinish.NAME_MESSAGE,
                    message)
                putExtra(
                    Constant.Intent.ClearDataFinish.NAME_PACKAGE_NAME,
                    pkgName)
            }
        )
    }
}
