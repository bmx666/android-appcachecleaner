package com.github.bmx666.appcachecleaner.util

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.service.AppInfoService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


abstract class BaseLocalBroadcastManagerHelper(
    protected val context: Context,
    private val eventBus: EventBus,
) {

    /** Handle an event delivered on the main thread. */
    protected abstract fun onEvent(event: AppEvent)

    // Main-thread scope so callbacks run where the old BroadcastReceiver did.
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var collectJob: Job? = null

    protected fun register() {
        collectJob = scope.launch {
            eventBus.events.collect { onEvent(it) }
        }
    }

    fun onDestroy() {
        collectJob?.cancel()
        collectJob = null
        scope.cancel()
    }

    fun sendBroadcast(event: AppEvent) {
        eventBus.emit(event)
    }
}

interface IIntentActivityCallback {
    fun onClearCacheFinish(message: String?)
    fun onClearDataFinish(message: String?)
    fun onStopAccessibilityServiceFeedback()
}

class LocalBroadcastManagerActivityHelper(
    context: Context,
    eventBus: EventBus,
    private val callback: IIntentActivityCallback): BaseLocalBroadcastManagerHelper(context, eventBus) {

    override fun onEvent(event: AppEvent) {
        when (event) {
            is AppEvent.AppInfo -> {
                val pkgName = event.packageName
                if (BuildConfig.DEBUG)
                    Logger.d("[Activity] AppInfo: package name = $pkgName")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val serviceIntent = Intent(
                        context,
                        AppInfoService::class.java).apply {
                        putExtra(
                            Constant.Intent.AppInfo.NAME_PACKAGE_NAME,
                            pkgName)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                } else {
                    ActivityHelper.startApplicationDetailsActivity(context, pkgName)
                }
            }
            is AppEvent.ClearCacheFinish -> {
                if (BuildConfig.DEBUG)
                    Logger.d("[Activity] ClearCacheFinish: message = ${event.message}, pkgName = ${event.packageName}")
                callback.onClearCacheFinish(event.message)
            }
            is AppEvent.ClearDataFinish -> {
                if (BuildConfig.DEBUG)
                    Logger.d("[Activity] ClearDataFinish: message = ${event.message}, pkgName = ${event.packageName}")
                callback.onClearDataFinish(event.message)
            }
            AppEvent.StopAccessibilityServiceFeedback -> {
                if (BuildConfig.DEBUG)
                    Logger.d("[Activity] StopAccessibilityServiceFeedback")
                callback.onStopAccessibilityServiceFeedback()
            }
            else -> Unit
        }
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
            sendBroadcast(AppEvent.StopAccessibilityService)
    }

    fun sendPackageListToClearCache(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG) {
            Logger.d("[Activity] sendPackageListToClearCache")
            pkgList.forEach {
                Logger.d("[Activity] sendPackageListToClearCache: package name = $it")
            }
        }
        sendBroadcast(AppEvent.ClearCache(pkgList))
    }

    fun sendPackageListToClearData(pkgList: ArrayList<String>) {
        if (BuildConfig.DEBUG) {
            Logger.d("[Activity] sendPackageListToClearData")
            pkgList.forEach {
                Logger.d("[Activity] sendPackageListToClearData: package name = $it")
            }
        }
        sendBroadcast(AppEvent.ClearData(pkgList))
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
    eventBus: EventBus,
    private val callback: IIntentServiceCallback): BaseLocalBroadcastManagerHelper(context, eventBus) {

    override fun onEvent(event: AppEvent) {
        when (event) {
            AppEvent.StopAccessibilityService -> {
                if (BuildConfig.DEBUG)
                    Logger.d("[Service] StopAccessibilityService")
                // send back to Activity - service was stopped
                sendBroadcast(AppEvent.StopAccessibilityServiceFeedback)
                callback.onStopAccessibilityService()
            }
            is AppEvent.ClearCache -> {
                val pkgList = event.packageList
                if (BuildConfig.DEBUG) {
                    Logger.d("[Service] ClearCache")
                    pkgList?.forEach {
                        Logger.d("[Service] ClearCache: package name = $it")
                    }
                }
                callback.onClearCache(pkgList)
            }
            is AppEvent.ClearCacheFinish -> {
                if (BuildConfig.DEBUG)
                    Logger.d("[Service] ClearCacheFinish")
                callback.onClearCacheFinish()
            }
            is AppEvent.ClearData -> {
                val pkgList = event.packageList
                if (BuildConfig.DEBUG) {
                    Logger.d("[Service] ClearData")
                    pkgList?.forEach {
                        Logger.d("[Service] ClearData: package name = $it")
                    }
                }
                callback.onClearData(pkgList)
            }
            is AppEvent.ClearDataFinish -> {
                if (BuildConfig.DEBUG)
                    Logger.d("[Service] CleanDataFinish")
                callback.onClearDataFinish()
            }
            else -> Unit
        }
    }

    init {
        register()
    }

    fun sendAppInfo(pkgName: String) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendAppInfo: package name = $pkgName")
        sendBroadcast(AppEvent.AppInfo(pkgName))
    }

    fun sendFinishClearCache(message: String?,
                   pkgName: String?) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendFinishClearCache: message = $message, pkgName = $pkgName")
        sendBroadcast(AppEvent.ClearCacheFinish(message, pkgName))
    }

    fun sendFinishClearData(message: String?,
                             pkgName: String?) {
        if (BuildConfig.DEBUG)
            Logger.d("[Service] sendFinishClearData: message = $message, pkgName = $pkgName")
        sendBroadcast(AppEvent.ClearDataFinish(message, pkgName))
    }
}
