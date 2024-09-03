package com.github.bmx666.appcachecleaner.clearcache

import android.content.Context
import android.os.Build
import android.os.ConditionVariable
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.clearcache.scenario.BaseClearScenario
import com.github.bmx666.appcachecleaner.clearcache.scenario.DefaultClearScenario
import com.github.bmx666.appcachecleaner.clearcache.scenario.XiaomiMIUIClearScenario
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_IGNORE
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INIT
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_SYSTEM
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.CANCEL_INTERRUPTED_BY_USER
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH_FAILED
import com.github.bmx666.appcachecleaner.data.UserPrefExtraManager
import com.github.bmx666.appcachecleaner.data.UserPrefScenarioManager
import com.github.bmx666.appcachecleaner.data.UserPrefTimeoutManager
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.util.ExtraSearchTextHelper
import com.github.bmx666.appcachecleaner.util.showTree
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

class AccessibilityClearManager {

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

    private var selfPackageName: String? = null

    private data class NodeState(
        val className: CharSequence?,
        val viewId: CharSequence?,
        val children: List<NodeState> = emptyList()
    )

    // For Android 14 and later save Accessibility Node Info
    // to avoid spamming by Jetpack recomposition
    private var lastNodeState: NodeState? = null

    private enum class ClearType {
        CLEAR_CACHE,
        CLEAR_DATA,
    }

    private var clearType: ClearType? = null

    fun setClearTypeClearCache() {
        clearType = ClearType.CLEAR_CACHE
    }

    fun setClearTypeClearData() {
        clearType = ClearType.CLEAR_DATA
    }

    suspend fun setSettings(@ApplicationContext context: Context) {
        val userPrefScenarioManager = UserPrefScenarioManager(context)
        val userPrefTimeoutManager = UserPrefTimeoutManager(context)
        val userPrefExtraManager = UserPrefExtraManager(context)

        selfPackageName = context.packageName

        val scenario = userPrefScenarioManager.scenario.first()
        clearScenario =
            when (scenario) {
                Constant.Scenario.DEFAULT -> DefaultClearScenario()
                Constant.Scenario.XIAOMI_MIUI -> XiaomiMIUIClearScenario()
            }

        clearScenario.arrayTextClearCacheButton.addAll(
            ExtraSearchTextHelper.getTextForClearCache(context)
        )

        clearScenario.arrayTextClearDataButton.addAll(
            ExtraSearchTextHelper.getTextForClearData(context)
        )

        clearScenario.arrayTextStorageAndCacheMenu.addAll(
            ExtraSearchTextHelper.getTextForStorage(context)
        )

        clearScenario.arrayTextOkButton.addAll(
            ExtraSearchTextHelper.getTextForOk(context)
        )

        clearScenario.arrayTextCancelButton.addAll(
            ExtraSearchTextHelper.getTextForCancel(context)
        )

        clearScenario.arrayTextDeleteButton.addAll(
            ExtraSearchTextHelper.getTextForDelete(context)
        )

        clearScenario.arrayTextClearDataDialogTitle.addAll(
            ExtraSearchTextHelper.getTextForClearDataDialogTitle(context)
        )

        clearScenario.arrayTextForceStopButton.addAll(
            ExtraSearchTextHelper.getTextForForceStop(context)
        )

        clearScenario.arrayTextForceStopDialogTitle.addAll(
            ExtraSearchTextHelper.getTextForForceStopDialogTitle(context)
        )

        clearScenario.delayForNextAppTimeoutMs =
            userPrefTimeoutManager.delayForNextAppTimeout.first()

        clearScenario.maxWaitAppTimeoutMs =
            userPrefTimeoutManager.maxWaitAppTimeout.first()

        clearScenario.maxWaitClearCacheButtonTimeoutMs =
            userPrefTimeoutManager.maxWaitClearCacheButtonTimeout.first()

        clearScenario.maxWaitAccessibilityEventMs =
            userPrefTimeoutManager.maxWaitAccessibilityEventTimeout.first()

        clearScenario.goBackAfterApps =
            userPrefTimeoutManager.maxGoBackAfterApps.first()

        clearScenario.forceStopApps =
            userPrefExtraManager.actionForceStopApps.first()
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

                    clearScenario.resetInternalState()
                    // avoid self force stop
                    if (currentPkg == selfPackageName)
                        clearScenario.forceStopTries = 0

                    if (index > 0) {
                        waitNextAppJob?.cancel(CANCEL_IGNORE)
                        waitNextAppJob = ioScope.launch {
                            val timeoutMs = clearScenario.delayForNextAppTimeoutMs.toLong()
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
                            val timeoutMs = clearScenario.maxWaitAccessibilityEventMs.toLong()
                            delay(timeoutMs)
                            Logger.w("Accessibility Event timeout")
                        }
                        waitAccessibilityJob?.join()
                        // got first Accessibility Event
                        if (waitAccessibilityJob?.isCancelled == true) {
                            val timeoutMs = clearScenario.maxWaitAppTimeoutMs.toLong()
                            delay(timeoutMs)
                        }
                    }
                    packageJob?.join()

                    // timeout, no accessibility events, move to the next app
                    accessibilityJob?.cancel(CANCEL_IGNORE)

                    updatePosition(index + 1)

                    // got first Accessibility event, need go back
                    if (waitAccessibilityJob?.isCancelled == true) {
                        val goBackAfterApps = clearScenario.goBackAfterApps
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
            // force clear type to avoid misbehavior
            clearType = null
        }
    }

    fun clearDataApp(pkgList: ArrayList<String>,
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
                        Logger.d("clearDataApp: package name = $pkg")

                    currentPkg = pkg

                    updatePosition(index)

                    if (pkg.trim().isEmpty())
                        continue

                    clearScenario.resetInternalState()
                    // avoid self force stop
                    if (currentPkg == selfPackageName)
                        clearScenario.forceStopTries = 0

                    if (index > 0) {
                        waitNextAppJob?.cancel(CANCEL_IGNORE)
                        waitNextAppJob = ioScope.launch {
                            val timeoutMs = clearScenario.delayForNextAppTimeoutMs.toLong()
                            delay(timeoutMs)
                        }
                        waitNextAppJob?.join()
                    }

                    if (BuildConfig.DEBUG)
                        Logger.d("clearDataApp: open AppInfo of $pkg")
                    openAppInfo(pkg)

                    // wait cache clean process
                    packageJob = ioScope.launch {
                        // wait first Accessibility Event - open AppInfo
                        waitAccessibilityJob = ioScope.launch {
                            val timeoutMs = clearScenario.maxWaitAccessibilityEventMs.toLong()
                            delay(timeoutMs)
                            Logger.w("Accessibility Event timeout")
                        }
                        waitAccessibilityJob?.join()
                        // got first Accessibility Event
                        if (waitAccessibilityJob?.isCancelled == true) {
                            val timeoutMs = clearScenario.maxWaitAppTimeoutMs.toLong()
                            delay(timeoutMs)
                        }
                    }
                    packageJob?.join()

                    // timeout, no accessibility events, move to the next app
                    accessibilityJob?.cancel(CANCEL_IGNORE)

                    updatePosition(index + 1)

                    // got first Accessibility event, need go back
                    if (waitAccessibilityJob?.isCancelled == true) {
                        val goBackAfterApps = clearScenario.goBackAfterApps
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
            // force clear type to avoid misbehavior
            clearType = null
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

                // Jetpack compose spam Accessibility Service when update some text
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val currentNodeState = captureNodeState(nodeInfo)
                    if (lastNodeState != null && compareNodeStates(lastNodeState!!, currentNodeState)) {
                        // If the state is identical, ignore this event
                        Logger.w("ignore recomposition event")
                        return
                    }

                    // Update the last known state to the current one
                    lastNodeState = currentNodeState
                }

                if (BuildConfig.DEBUG) {
                    Logger.d("===>>> TREE BEGIN <<<===")
                    nodeInfo.showTree(event.eventTime, 0)
                    Logger.d("===>>> TREE END <<<===")
                }

                when (clearType) {
                    ClearType.CLEAR_CACHE -> doAccessibilityEventClearCache(nodeInfo)
                    ClearType.CLEAR_DATA -> doAccessibilityEventClearData(nodeInfo)
                    else -> {
                        // interrupt misbehavior
                        interruptBySystem()
                    }
                }
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
            val timeoutMs = clearScenario.maxWaitAccessibilityEventMs.toLong()
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

    private fun doAccessibilityEventClearCache(nodeInfo: AccessibilityNodeInfo) {
        accessibilityJob?.cancel(CANCEL_IGNORE)
        accessibilityJob = ioScope.launch {
            try {
                waitAccessibilityJob?.cancel()
                val result = clearScenario.doClearCache(nodeInfo)
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

    private fun doAccessibilityEventClearData(nodeInfo: AccessibilityNodeInfo) {
        accessibilityJob?.cancel(CANCEL_IGNORE)
        accessibilityJob = ioScope.launch {
            try {
                waitAccessibilityJob?.cancel()
                val result = clearScenario.doClearData(nodeInfo)
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

    private fun captureNodeState(nodeInfo: AccessibilityNodeInfo): NodeState {
        val childStates = mutableListOf<NodeState>()
        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i)
            if (child != null) {
                childStates.add(captureNodeState(child))
            }
        }
        return NodeState(
            className = nodeInfo.className,
            viewId = nodeInfo.viewIdResourceName,
            children = childStates
        )
    }

    private fun compareNodeStates(oldState: NodeState, newState: NodeState): Boolean {
        if (oldState.className != newState.className || oldState.viewId != newState.viewId) {
            return false
        }

        if (oldState.children.size != newState.children.size) {
            return false
        }

        for (i in oldState.children.indices) {
            if (!compareNodeStates(oldState.children[i], newState.children[i])) {
                return false
            }
        }

        return true
    }

    companion object {
        private var clearScenario: BaseClearScenario = DefaultClearScenario()
    }
}
