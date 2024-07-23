package com.github.bmx666.appcachecleaner.clearcache

import android.os.ConditionVariable
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.clearcache.scenario.BaseClearCacheScenario
import com.github.bmx666.appcachecleaner.clearcache.scenario.DefaultClearCacheScenario
import com.github.bmx666.appcachecleaner.clearcache.scenario.XiaomiMIUIClearCacheScenario
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_IGNORE
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INIT
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_SYSTEM
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_USER
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH_FAILED
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.util.showTree
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

class AccessibilityClearCacheManager {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    // main job that starts and finish cache clean process of all packages
    private var mainJob: Job? = null
    // package job that starts and finish for one package
    private var packageJob: Job? = null
    // accessibility event job that starts when Accessibility service got event
    private var accessibilityJob: Job? = null
    // wait accessibility event job
    private var waitAccessibilityJob: Job? = null
    // wait for the next app job
    private var waitNextAppJob: Job? = null
    // For Android 14 and later need go back to reduce windows stack
    private var goBackJob: Job? = null
    private val needGoBack = ConditionVariable()

    data class Settings(
        val clearCacheTextList: ArrayList<CharSequence>,
        val clearDataTextList: ArrayList<CharSequence>,
        val storageTextList: ArrayList<CharSequence>,
        val okTextList: ArrayList<CharSequence>,
        val delayForNextAppTimeout: Int?,
        val maxWaitAppTimeout: Int?,
        val maxWaitClearCacheButtonTimeout: Int?,
        val maxWaitAccessibilityEventTimeout: Int?,
        val goBackAfterApps: Int?,
    )

    fun setSettings(scenario: Constant.Scenario?, settings: Settings) {
        scenario?.let {
            cacheCleanScenario =
                when (it) {
                    Constant.Scenario.DEFAULT -> DefaultClearCacheScenario()
                    Constant.Scenario.XIAOMI_MIUI -> XiaomiMIUIClearCacheScenario()
                }
        }

        cacheCleanScenario.setExtraSearchText(
            settings.clearCacheTextList,
            settings.clearDataTextList,
            settings.storageTextList,
            settings.okTextList)

        settings.maxWaitAppTimeout?.let {
            cacheCleanScenario.maxWaitAppTimeoutMs = it * 1000
        }

        settings.maxWaitClearCacheButtonTimeout?.let {
            cacheCleanScenario.maxWaitClearCacheButtonTimeoutMs = it * 1000
        }

        settings.delayForNextAppTimeout?.let {
            cacheCleanScenario.delayForNextAppTimeoutMs = it * 1000
        }

        settings.maxWaitAccessibilityEventTimeout?.let {
            cacheCleanScenario.maxWaitAccessibilityEventMs = it * 1000
        }

        settings.goBackAfterApps?.let {
            cacheCleanScenario.goBackAfterApps = it
        }
    }

    fun clearCacheApp(pkgList: ArrayList<String>,
                      updatePosition: (Int) -> Unit,
                      performBack: () -> Boolean,
                      openAppInfo: KFunction1<String, Unit>,
                      finish: (String?, String?) -> Unit) {

        accessibilityJob?.cancel(CANCEL_INIT)
        packageJob?.cancel(CANCEL_INIT)
        mainJob?.cancel(CANCEL_INIT)

        mainJob = ioScope.launch {
            var currentPkg: String? = null

            try {

                for ((index, pkg) in pkgList.withIndex()) {
                    if (BuildConfig.DEBUG)
                        Logger.d("clearCacheApp: package name = $pkg")

                    currentPkg = pkg

                    updatePosition(index)

                    if (pkg.trim().isEmpty())
                        continue

                    cacheCleanScenario.resetInternalState()

                    if (index > 0) {
                        waitNextAppJob?.cancel(CANCEL_IGNORE)
                        waitNextAppJob = ioScope.launch {
                            val timeoutMs = cacheCleanScenario.delayForNextAppTimeoutMs.toLong()
                            delay(timeoutMs)
                        }
                        waitNextAppJob?.join()
                    }

                    if (BuildConfig.DEBUG)
                        Logger.d("clearCacheApp: open AppInfo of $pkg")
                    openAppInfo(pkg)

                    // wait cache clean process
                    packageJob = ioScope.launch {
                        // wait first Accessibility Event - open AppInfo
                        waitAccessibilityJob = ioScope.launch {
                            val timeoutMs = cacheCleanScenario.maxWaitAccessibilityEventMs.toLong()
                            delay(timeoutMs)
                            Logger.w("Accessibility Event timeout")
                        }
                        waitAccessibilityJob?.join()
                        // got first Accessibility Event
                        if (waitAccessibilityJob?.isCancelled == true) {
                            val timeoutMs = cacheCleanScenario.maxWaitAppTimeoutMs.toLong()
                            delay(timeoutMs)
                        }
                    }
                    packageJob?.join()

                    // timeout, no accessibility events, move to the next app
                    accessibilityJob?.cancel(CANCEL_IGNORE)

                    // got first Accessibility event, need go back
                    if (waitAccessibilityJob?.isCancelled == true) {
                        val goBackAfterApps = cacheCleanScenario.goBackAfterApps
                        if (goBackAfterApps > 0) {
                            // go back after each Nth apps and for the last app
                            if ((index % goBackAfterApps == 0 && index != 0) or (index == pkgList.size - 1))
                                doGoBack(performBack)
                        }
                    }
                }

                finish(null, null)

            } catch (e: CancellationException) {
                when (e.message) {
                    CANCEL_IGNORE.message, CANCEL_INIT.message -> {}
                    else -> finish(e.message, currentPkg)
                }
            }
        }
    }

    fun checkEvent(event: AccessibilityEvent) {
        // do cache clean only if processing package
        if (mainJob?.isActive == true) {
            if (goBackJob?.isActive == true) {
                needGoBack.open()
            } else if (packageJob?.isActive == true) {
                if (event.source == null)
                    return

                val nodeInfo = event.source!!

                if (BuildConfig.DEBUG) {
                    Logger.d("===>>> TREE BEGIN <<<===")
                    nodeInfo.showTree(0)
                    Logger.d("===>>> TREE END <<<===")
                }

                doAccessibilityEvent(nodeInfo)
            }
        }
    }

    fun interruptByUser() {
        accessibilityJob?.cancel(CANCEL_INTERRUPTED_BY_USER)
        packageJob?.cancel(CANCEL_INTERRUPTED_BY_USER)
        mainJob?.cancel(CANCEL_INTERRUPTED_BY_USER)

        waitAccessibilityJob?.cancel(CANCEL_IGNORE)
        waitNextAppJob?.cancel(CANCEL_IGNORE)
        goBackJob?.cancel(CANCEL_IGNORE)
    }

    fun interruptBySystem() {
        accessibilityJob?.cancel(CANCEL_INTERRUPTED_BY_SYSTEM)
        packageJob?.cancel(CANCEL_INTERRUPTED_BY_SYSTEM)
        mainJob?.cancel(CANCEL_INTERRUPTED_BY_SYSTEM)

        waitAccessibilityJob?.cancel(CANCEL_IGNORE)
        waitNextAppJob?.cancel(CANCEL_IGNORE)
        goBackJob?.cancel(CANCEL_IGNORE)
    }

    private suspend fun doGoBack(performBack: () -> Boolean) {
        goBackJob?.cancel(CANCEL_IGNORE)
        goBackJob = ioScope.launch {
            val timeoutMs = cacheCleanScenario.maxWaitAccessibilityEventMs.toLong()
            while (true) {
                performBack()
                needGoBack.close()
                // need more go back
                if (needGoBack.block(timeoutMs))
                    continue
                break
            }
        }
        goBackJob?.join()
    }

    private fun doAccessibilityEvent(nodeInfo: AccessibilityNodeInfo) {
        accessibilityJob?.cancel(CANCEL_IGNORE)
        accessibilityJob = ioScope.launch {
            try {
                waitAccessibilityJob?.cancel()
                val result = cacheCleanScenario.doCacheClean(nodeInfo)
                when (result?.message) {
                    PACKAGE_FINISH.message,
                    PACKAGE_FINISH_FAILED.message,
                    -> { packageJob?.cancel(result) }
                    else -> {}
                }
            } catch (e: CancellationException) {
                // TODO: process exceptions
            }
        }
    }

    companion object {
        private var cacheCleanScenario: BaseClearCacheScenario = DefaultClearCacheScenario()
    }
}
