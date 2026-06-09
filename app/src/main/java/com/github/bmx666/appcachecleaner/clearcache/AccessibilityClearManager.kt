package com.github.bmx666.appcachecleaner.clearcache

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.clearcache.node.AndroidNodeView
import com.github.bmx666.appcachecleaner.clearcache.node.NodeView
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
import com.github.bmx666.appcachecleaner.platform.DefaultDispatcherProvider
import com.github.bmx666.appcachecleaner.platform.DispatcherProvider
import com.github.bmx666.appcachecleaner.util.ExtraSearchTextHelper
import com.github.bmx666.appcachecleaner.util.showTree
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

// Injectable seams (default to production impls so the static service singleton constructs
// with `AccessibilityClearManager()` unchanged):
//  - [scenario]     : the clear scenario. setSettings() rebuilds it from user prefs in prod;
//                     tests inject a real DefaultClearScenario (or a recording stub) and skip
//                     setSettings, scripting node trees directly.
//  - [dispatchers]  : coordination + scenario-hop dispatchers. Tests pass a single
//                     TestDispatcherProvider so the whole flow runs on virtual time under
//                     runTest (deterministic, no real Main/IO pools). NOTE: prod now uses
//                     dispatchers.main (Dispatchers.Main), not Main.immediate as before -
//                     the documented single-thread confinement still holds (all coordination
//                     state is touched only from this scope), the trade is testability.
class AccessibilityClearManager internal constructor(
    private val scenario: BaseClearScenario = DefaultClearScenario(),
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) {

    // Single coordination scope on dispatchers.main: all coordination state below is
    // confined to the one accessibility main thread (clearApp body, checkEvent producer,
    // doGoBack), so the fields need no memory barrier (was 6 cross-thread Job fields).
    // SupervisorJob so one failed child does not tear down the scope. Recreatable:
    // destroy() cancels it on service teardown, and clearCacheApp/clearDataApp revive it
    // because this manager is a static singleton reused across service rebinds. Only the
    // scenario node-walk hops off this thread (withContext(dispatchers.io)).
    private fun newScope() = CoroutineScope(SupervisorJob() + dispatchers.main)
    private var coordScope = newScope()

    // The whole-run job. Its children (per-package waits, scenario hop, go-back loop) are
    // structured under it, so a single cancel cascades - no per-job enumeration anymore.
    private var mainJob: Job? = null

    // Callback -> coroutine bridge. checkEvent (producer) drops the current screen's node
    // / a go-back signal into the queue for the active phase; the clear loop consumes it.
    // CONFLATED keeps only the newest item (matches the old "new event preempts the
    // in-flight one"). eventChannel is replaced fresh per package -> zero stale carryover.
    private var eventChannel: Channel<NodeView>? = null
    private var goBackChannel: Channel<Unit>? = null

    // Routes checkEvent. Package -> feed eventChannel; GoBack -> feed goBackChannel;
    // Idle -> drop (between packages / before-after a run). Replaces inspecting
    // mainJob/packageJob/goBackJob isActive. Main-confined like the channels above.
    private enum class Phase { Idle, Package, GoBack }
    private var phase: Phase = Phase.Idle

    private var selfPackageName: String? = null

    // Safety cap on go-back presses: bounds the loop if a screen swallows BACK and the
    // foreground never returns to our app (otherwise it would spin forever). Comfortably
    // above any real Settings back-stack depth accrued between go-back points.
    private val MAX_GO_BACK_TRIES = 16

    // Instance-scoped (was a companion static): per-manager clear scenario, rebuilt by
    // setSettings(). Keeping it on the instance removes cross-instance/rebind sharing.
    // Seeded from the injected [scenario] so tests can drive a known scenario without prefs.
    private var clearScenario: BaseClearScenario = scenario

    private data class NodeState(
        val className: CharSequence?,
        val viewId: CharSequence?,
        val children: List<NodeState> = emptyList()
    )

    // For Android 14 and later save Accessibility Node Info
    // to avoid spamming by Jetpack recomposition
    private var lastNodeState: NodeState? = null

    // Explicit clear-run state machine (was nullable ClearType enum). Set at clearApp
    // entry, reset to Idle when the run ends. `logTag` labels debug logs per type.
    // @Volatile retained as a defensive barrier: it is captured into a local on the main
    // thread before the IO scenario hop, but the run type is also read from checkEvent.
    private sealed class ClearState(val logTag: String) {
        object Idle : ClearState("idle")
        object ClearingCache : ClearState("clearCacheApp")
        object ClearingData : ClearState("clearDataApp")
    }

    @Volatile
    private var clearState: ClearState = ClearState.Idle

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
                      getForegroundPackageName: () -> String?,
                      openAppInfo: (String) -> Unit,
                      finish: (String?, String?) -> Unit) =
        clearApp(ClearState.ClearingCache, pkgList, updatePosition, performBack,
            getForegroundPackageName, openAppInfo, finish)

    fun clearDataApp(pkgList: ArrayList<String>,
                     updatePosition: (Int) -> Unit,
                     performBack: () -> Boolean,
                     getForegroundPackageName: () -> String?,
                     openAppInfo: (String) -> Unit,
                     finish: (String?, String?) -> Unit) =
        clearApp(ClearState.ClearingData, pkgList, updatePosition, performBack,
            getForegroundPackageName, openAppInfo, finish)

    // Per-package lifecycle driver: open AppInfo -> wait first event -> drive scenario
    // until finish/timeout -> go back -> next. Type-agnostic; cache-vs-data branching is
    // dispatched out via clearState -> scenario.doClearCache/doClearData. `state` sets the
    // run type (own log tag) and is reset to Idle when the run ends.
    private fun clearApp(state: ClearState,
                         pkgList: ArrayList<String>,
                         updatePosition: (Int) -> Unit,
                         performBack: () -> Boolean,
                         getForegroundPackageName: () -> String?,
                         openAppInfo: (String) -> Unit,
                         finish: (String?, String?) -> Unit) {
        val tag = state.logTag
        clearState = state
        // Revive scope if a prior destroy() cancelled it (static singleton reuse).
        if (!coordScope.isActive) coordScope = newScope()
        // Preempt any in-flight run; its children cancel with it (structured).
        mainJob?.cancel(CANCEL_INIT)

        mainJob = coordScope.launch {
            var currentPkg: String? = null

            try {

                for ((index, pkg) in pkgList.withIndex()) {
                    if (BuildConfig.DEBUG)
                        Logger.d("$tag: package name = $pkg")

                    currentPkg = pkg

                    updatePosition(index)

                    if (pkg.trim().isEmpty())
                        continue

                    clearScenario.resetInternalState()
                    // avoid self force stop
                    if (currentPkg == selfPackageName)
                        clearScenario.forceStopTries = 0

                    if (index > 0) {
                        // Idle during the inter-app delay -> stale/late events dropped.
                        phase = Phase.Idle
                        delay(clearScenario.delayForNextAppTimeoutMs.toLong().milliseconds)
                    }

                    // Fresh per-package queue: no event from the previous package can be
                    // mistaken for this package's first event (matters for system
                    // packages that open nothing -> must time out, not grab a stale node).
                    val channel = Channel<NodeView>(Channel.CONFLATED)
                    eventChannel = channel
                    phase = Phase.Package

                    if (BuildConfig.DEBUG)
                        Logger.d("$tag: open AppInfo of $pkg")
                    openAppInfo(pkg)

                    val gotEvent = processPackage(channel)

                    phase = Phase.Idle
                    updatePosition(index + 1)

                    // got first Accessibility event, need go back
                    if (gotEvent) {
                        val goBackAfterApps = clearScenario.goBackAfterApps
                        if (goBackAfterApps > 0) {
                            // go back after each Nth apps and for the last app
                            if ((index % goBackAfterApps == 0 && index != 0) or (index == pkgList.size - 1))
                                doGoBack(performBack, getForegroundPackageName)
                        }
                    }
                }

                finish(null, null)

            } catch (e: CancellationException) {
                when (e.message) {
                    CANCEL_IGNORE.message, CANCEL_INIT.message -> {}
                    else -> finish(e.message, currentPkg)
                }
            } finally {
                // Only the current run resets shared state; a run preempted by CANCEL_INIT
                // must not clobber the run that replaced it (its finally runs later).
                if (mainJob === coroutineContext[Job]) {
                    phase = Phase.Idle
                    eventChannel = null
                    // force clear type to avoid misbehavior
                    clearState = ClearState.Idle
                }
            }
        }
    }

    // Drive one package. Returns true if at least one accessibility event arrived (gates
    // go-back). Waits maxWaitAccessibilityEventMs for the first event; none -> the package
    // opened nothing (e.g. a system package) -> skip. After the first event, feed each
    // node to the scenario until it reports finish/failed or maxWaitAppTimeoutMs elapses.
    private suspend fun processPackage(channel: Channel<NodeView>): Boolean {
        val first: NodeView =
            withTimeoutOrNull(clearScenario.maxWaitAccessibilityEventMs.toLong().milliseconds) {
                channel.receive()
            } ?: run {
                Logger.w("Accessibility Event timeout")
                return false
            }
        // run type captured on the main thread, then used inside the IO scenario hop
        val type = clearState
        withTimeoutOrNull(clearScenario.maxWaitAppTimeoutMs.toLong().milliseconds) {
            var node: NodeView = first
            while (true) {
                // stable val: a captured var cannot be smart-cast inside the IO closure
                val current = node
                val result = withContext(dispatchers.io) {
                    when (type) {
                        ClearState.ClearingCache -> clearScenario.doClearCache(current)
                        ClearState.ClearingData -> clearScenario.doClearData(current)
                        // event during active run but no type -> misbehavior, stop package
                        ClearState.Idle -> PACKAGE_FINISH_FAILED
                    }
                }
                when (result?.message) {
                    PACKAGE_FINISH.message,
                    PACKAGE_FINISH_FAILED.message,
                    -> return@withTimeoutOrNull
                    else -> {}
                }
                node = channel.receive()
            }
        }
        return true
    }

    // Framework entry point: adapt the live accessibility node and delegate. Kept thin so
    // the whole phase-routing + recomposition-dedup decision logic in onWindowStateChanged
    // is exercisable on a plain JVM with a scripted NodeView (no AccessibilityEvent).
    fun checkEvent(event: AccessibilityEvent) {
        onWindowStateChanged(event.source?.let { AndroidNodeView(it) })
    }

    // Routes one window-state change. [node] is the active screen's root for the Package
    // phase (null -> nothing to feed) and is ignored for the GoBack phase (which only needs
    // the "window changed" signal). Tests drive this directly: a scripted tree for Package
    // events, any value for the go-back loop's window-change pings.
    internal fun onWindowStateChanged(node: NodeView?) {
        when (phase) {
            // back-press loop is waiting for the window to change
            Phase.GoBack -> goBackChannel?.trySend(Unit)
            Phase.Package -> {
                val nodeView = node ?: return

                // Jetpack compose spam Accessibility Service when update some text
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val currentNodeState = captureNodeState(nodeView)
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
                    nodeView.showTree(0, 0)
                    Logger.d("===>>> TREE END <<<===")
                }

                // hand the node to the active package loop (CONFLATED -> newest wins)
                eventChannel?.trySend(nodeView)
            }
            // between packages / outside a run -> nothing to do
            Phase.Idle -> {}
        }
    }

    fun interruptByUser() {
        // structured cancellation cascades to all children of the run
        mainJob?.cancel(CANCEL_INTERRUPTED_BY_USER)
    }

    fun interruptBySystem() {
        mainJob?.cancel(CANCEL_INTERRUPTED_BY_SYSTEM)
    }

    // Service teardown hook: stop in-flight work and cancel the scope so no coroutine
    // outlives the service. Scope is revived on the next clearCacheApp/clearDataApp.
    fun destroy() {
        interruptBySystem()
        coordScope.cancel()
    }

    private suspend fun doGoBack(performBack: () -> Boolean,
                                getForegroundPackageName: () -> String?) {
        val channel = Channel<Unit>(Channel.CONFLATED)
        goBackChannel = channel
        phase = Phase.GoBack
        val timeoutMs = clearScenario.maxWaitAccessibilityEventMs.toLong()
        try {
            // Pop the back stack until OUR app is the active window. Foreground is read
            // DIRECTLY (rootInActiveWindow.packageName) each iteration, not inferred from
            // events, because our own app does NOT reliably emit a window-state event on
            // return. Consequences:
            //  - silent return to our app can't hang (no event needed to detect self),
            //  - stale events can't over-back (real window re-checked before each back),
            //  - guard cap stops a screen that swallows BACK from spinning forever.
            var tries = 0
            while (getForegroundPackageName() != selfPackageName && tries++ < MAX_GO_BACK_TRIES) {
                performBack()
                // pace the loop: wait for the window to change, then re-read the real
                // foreground. timeout is just pacing - the read is the source of truth.
                withTimeoutOrNull(timeoutMs.milliseconds) { channel.receive() }
            }
        } finally {
            phase = Phase.Idle
            goBackChannel = null
        }
    }

    private fun captureNodeState(nodeInfo: NodeView): NodeState {
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
}
