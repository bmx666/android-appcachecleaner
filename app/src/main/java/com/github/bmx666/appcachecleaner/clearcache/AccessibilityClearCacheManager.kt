package com.github.bmx666.appcachecleaner.clearcache

import android.view.accessibility.AccessibilityEvent
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.clearcache.scenario.BaseClearCacheScenario
import com.github.bmx666.appcachecleaner.clearcache.scenario.DefaultClearCacheScenario
import com.github.bmx666.appcachecleaner.clearcache.scenario.XiaomiMIUIClearCacheScenario
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.util.showTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

class AccessibilityClearCacheManager {

    data class Settings(
        val clearCacheTextList: ArrayList<CharSequence>,
        val clearDataTextList: ArrayList<CharSequence>,
        val storageTextList: ArrayList<CharSequence>,
        val okTextList: ArrayList<CharSequence>,
        val maxWaitAppTimeout: Int?,
        val maxWaitClearCacheButtonTimeout: Int?,
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
            cacheCleanScenario.setMaxWaitAppTimeout(it)
        }

        settings.maxWaitClearCacheButtonTimeout?.let {
            cacheCleanScenario.setMaxWaitClearCacheButtonTimeout(it)
        }
    }

    fun clearCacheApp(pkgList: ArrayList<String>,
                      updatePosition: (Int) -> Unit,
                      openAppInfo: KFunction1<String, Unit>,
                      finish: KFunction1<Boolean, Unit>) {

        cacheCleanScenario.stateMachine.init()

        for ((index, pkg) in pkgList.withIndex()) {
            if (BuildConfig.DEBUG)
                Logger.d("clearCacheApp: package name = $pkg")

            updatePosition(index)

            // everything is possible...
            if (pkg.trim().isEmpty()) continue

            // state not changes, something goes wrong...
            if (cacheCleanScenario.stateMachine.isInterrupted()) break

            cacheCleanScenario.stateMachine.setOpenAppInfo()
            if (BuildConfig.DEBUG)
                Logger.d("clearCacheApp: open AppInfo")
            openAppInfo(pkg)

            // state not changes, something goes wrong...
            if (cacheCleanScenario.stateMachine.isInterrupted()) break

            cacheCleanScenario.processState()

            // something goes wrong...
            if (cacheCleanScenario.stateMachine.isInterrupted()) break
        }

        val interrupted = cacheCleanScenario.stateMachine.isInterrupted()
        cacheCleanScenario.stateMachine.init()

        finish(interrupted)
    }

    fun checkEvent(event: AccessibilityEvent) {

        if (cacheCleanScenario.stateMachine.isDone()) return

        if (event.source == null) {
            cacheCleanScenario.stateMachine.setFinishCleanApp()
            return
        }

        val nodeInfo = event.source!!

        if (BuildConfig.DEBUG) {
            Logger.d("===>>> TREE BEGIN <<<===")
            nodeInfo.showTree(0)
            Logger.d("===>>> TREE END <<<===")
        }

        CoroutineScope(Dispatchers.IO).launch {
            cacheCleanScenario.doCacheClean(nodeInfo)
        }
    }

    fun interrupt() {
        if (cacheCleanScenario.stateMachine.isDone()) return
        cacheCleanScenario.stateMachine.setInterrupted()
    }

    companion object {
        private var cacheCleanScenario: BaseClearCacheScenario = DefaultClearCacheScenario()
    }
}
