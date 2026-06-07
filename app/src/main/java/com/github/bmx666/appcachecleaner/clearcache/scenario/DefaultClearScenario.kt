package com.github.bmx666.appcachecleaner.clearcache.scenario

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH_FAILED
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_WAIT_DIALOG
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_WAIT_NEXT_STEP
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_FORCE_STOP_TRIES
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.util.findByViewIdResourceName
import com.github.bmx666.appcachecleaner.util.findClickable
import com.github.bmx666.appcachecleaner.util.findNode
import com.github.bmx666.appcachecleaner.util.showTree
import com.github.bmx666.appcachecleaner.util.takeIfMatches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/***
 * Steps to clean app cache:
 *
 * 1. open "App Info" settings for specific package
 * 2. if Accessibility event timeout has been expired, goto #6
 * 3. find "clear cache button":
 *    NOTE: some Settings UI can have it on "App Info" settings
 *    3.1. "clear cache button" not found, goto #4
 *    3.2. "clear cache button" found:
 *       3.2.1. if button is enabled, click it:
 *          3.2.1.1. if perform click was success, goto #6
 *          3.2.1.2. if perform click failed, goto #7
 *       3.2.2. if button is disabled (cache not calculated yet or 0 bytes), goto #6
 * 4. find "storage menu":
 *    4.1. "storage menu" not found, goto #6
 *    4.2. "storage menu" found:
 *       4.2.1. if menu is enabled, click it:
 *          4.2.1.1. if perform click was success, switched on "Storage Info" and goto #2
 *          4.2.1.2. if perform click failed, goto #7
 *       4.2.2. if menu was disabled, goto #6
 * 5. find RecyclerView:
 *    5.1. RecyclerView not found, goto #6
 *    5.2. RecyclerView found:
 *       5.2.1. if RecyclerView is enabled, scroll forward it:
 *          5.2.1.1. if perform scroll forward was success:
 *             5.2.1.1.1. wait minimal timeout for perform action
 *             5.2.1.1.2. if RecyclerView refresh failed, goto #6
 *             5.2.1.1.3. wait minimal timeout for perform action and goto #4
 *          5.2.1.2. if perform scroll forward failed, goto #6
 * 6. finish app clean process and move to the next
 * 7. interrupt clean process and halt
 ***/

internal class DefaultClearScenario: BaseClearScenario() {

    override fun resetInternalState() {
        forceStopTries = DEFAULT_FORCE_STOP_TRIES
        forceStopWaitDialog = false
    }

    private suspend fun findClearCacheButton(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        nodeInfo.findClickableByText(arrayTextClearCacheButton, SETTINGS_BUTTON_ID_REGEX)?.let { clearCacheButton ->

            // Android 7.1 and early does not support this feature
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var tries = maxWaitClearCacheButtonTimeoutMs / MIN_DELAY_PERFORM_CLICK_MS
                Logger.d("===>>> findClearCacheButton: tries = $tries <<<===")

                while (tries-- > 0) {
                    Logger.d("===>>> findClearCacheButton: loop: tries left = $tries <<<===")

                    if (clearCacheButton.isEnabled) {
                        Logger.d("===>>> findClearCacheButton: loop: is enabled, BREAK <<<===")
                        if (clearCacheButton.isClickable) {
                            Logger.d("===>>> findClearCacheButton: loop: is clickable, BREAK <<<===")
                            break
                        }
                    }

                    Logger.d("===>>> findClearCacheButton: loop: refresh, BEGIN <<<===")
                    if (!clearCacheButton.refresh()) {
                        Logger.d("===>>> findClearCacheButton: loop: refresh, FAIL <<<===")
                        return PACKAGE_FINISH_FAILED
                    }
                    Logger.d("===>>> findClearCacheButton: loop: refresh, END <<<===")

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)
                }
            }

            return when (doPerformClick(clearCacheButton, "clean cache button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> {
                    if (!nodeInfo.refresh()) {
                        Logger.w("clearCacheButton (no perform click): failed to refresh parent node")
                        return PACKAGE_FINISH_FAILED
                    }

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)
                    findClearCacheButton(nodeInfo)
                }
                true -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)

                        // something goes wrong...
                        if (!clearCacheButton.refresh()) {
                            Logger.w("clearCacheButton (perform click): failed to refresh")
                            return PACKAGE_FINISH_FAILED
                        }

                        // BUG: even after perform click nothing happens, do perform click again
                        if (clearCacheButton.isEnabled) {
                            Logger.w("clearCacheButton (perform click): still enabled")
                            if (!nodeInfo.refresh()) {
                                Logger.w("clearCacheButton (perform click): failed to refresh parent node")
                                return PACKAGE_FINISH_FAILED
                            }

                            Logger.w("clearCacheButton (perform click): try perform click again")
                            return findClearCacheButton(nodeInfo)
                        }
                    }

                    PACKAGE_FINISH
                }
                // move to the next app
                null -> PACKAGE_FINISH
            }
        }
        return null
    }

    private suspend fun findStorageAndCacheMenu(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        suspend fun fn(storageAndCacheMenu: AccessibilityNodeInfo): CancellationException? {
            return when (doPerformClick(storageAndCacheMenu, "storage & cache button")) {
                // open App Storage Activity
                true -> PACKAGE_WAIT_NEXT_STEP
                // move to the next app, storage & cache button disabled
                null -> PACKAGE_FINISH
                // storage & cache button was found, and it's enabled but perform click was failed
                false -> {
                    if (!nodeInfo.refresh())
                        return PACKAGE_FINISH_FAILED

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)
                    findStorageAndCacheMenu(nodeInfo)
                }
            }
        }

        nodeInfo.findStorageAndCacheMenu(arrayTextStorageAndCacheMenu)?.let { return fn(it) }

        return null
    }

    private suspend fun findForceStopButton(nodeInfo: AccessibilityNodeInfo): Boolean? {
        nodeInfo.findClickableByText(arrayTextForceStopButton, SETTINGS_BUTTON_ID_REGEX)?.let { forceStopButton ->

            return when (doPerformClick(forceStopButton, "force stop button")) {
                // force stop button was found, and it's enabled but perform click was failed
                // sometimes even with "false" force dialog could be open, so return true
                false -> true
                // wait force stop dialog
                true -> true
                // force stop disabled, or something goes wrong...
                null -> false
            }
        }

        return null
    }

    private fun findForceStopDialogTitle(nodeInfo: AccessibilityNodeInfo): Boolean {
        val foundTitle = nodeInfo.findDialogTitle(arrayTextForceStopDialogTitle) != null
        Logger.d("findForceStopDialogTitle: found title = $foundTitle")
        return foundTitle
    }

    private suspend fun findForceStopDialogOkButton(nodeInfo: AccessibilityNodeInfo): Boolean {
        nodeInfo.findDialogButton(arrayTextOkButton)?.let { forceStopDialogOkButton ->
            return when (doPerformClick(forceStopDialogOkButton, "force stop dialog - ok button")) {
                // ok button was found and it's enabled but perform click was failed
                false -> true
                // move to the next app
                else -> true
            }
        }
        return false
    }

    private suspend fun findClearDataButton(nodeInfo: AccessibilityNodeInfo): Boolean? {
        nodeInfo.findClickableByText(arrayTextClearDataButton, SETTINGS_BUTTON_ID_REGEX)?.let { clearDataButton ->
            when (doPerformClick(clearDataButton, "clear data button")) {
                // clear data button was found and it's enabled but perform click was failed
                false -> {
                    if (!nodeInfo.refresh()) {
                        Logger.w("clearDataButton (no perform click): failed to refresh parent node")
                        return false
                    }

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)
                    return findClearDataButton(nodeInfo)
                }
                true -> {
                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)
                    return true
                }
                null -> {
                    return false
                }
            }
        }
        return null
    }

    private suspend fun findClearDataDialogOkButton(nodeInfo: AccessibilityNodeInfo): Boolean? {
        nodeInfo.findDialogButton(arrayTextOkButton)?.let { clearDataDialogDeleteButton ->
            return when (doPerformClick(clearDataDialogDeleteButton, "clear data dialog - ok button")) {
                // ok button was found and it's enabled but perform click was failed
                false -> false
                // move to the next app
                else -> true
            }
        }
        return null
    }

    private suspend fun findClearDataDialogDeleteButton(nodeInfo: AccessibilityNodeInfo): Boolean? {
        nodeInfo.findDialogButton(arrayTextDeleteButton)?.let { clearDataDialogDeleteButton ->
            return when (doPerformClick(clearDataDialogDeleteButton, "clear data dialog - delete button")) {
                // ok button was found and it's enabled but perform click was failed
                false -> false
                // move to the next app
                else -> true
            }
        }
        return null
    }

    private fun findClearDataDialogTitle(nodeInfo: AccessibilityNodeInfo): Boolean {
        val foundTitle = nodeInfo.findDialogTitle(arrayTextClearDataDialogTitle) != null
        Logger.d("findClearDataDialogTitle: found title = $foundTitle")
        return foundTitle
    }

    private suspend fun processForceStop(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        if (!forceStopApps) return null
        if (forceStopTries <= 0) return null

        var foundForceStopDialog = findForceStopDialogTitle(nodeInfo)

        if (forceStopWaitDialog) {
            // force disable wait dialog to avoid misbehavior
            forceStopWaitDialog = false

            // For Android N MR1 and early need to wait dialog
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                // limit tries by app timeout
                var tries = maxWaitAppTimeoutMs / 250

                while (tries-- > 0) {
                    if (foundForceStopDialog)
                        break

                    nodeInfo.refresh()
                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)

                    if (BuildConfig.DEBUG) {
                        Logger.d("===>>> FORCE STOP Dialog TREE BEGIN <<<===")
                        nodeInfo.showTree(0, 0)
                        Logger.d("===>>> FORCE STOP Dialog TREE END <<<===")
                    }

                    foundForceStopDialog = findForceStopDialogTitle(nodeInfo)
                }
            }

            // Some Custom Force Stop Dialogs have buttons "Force stop" and "Cancel"
            if (!foundForceStopDialog) {
                val foundCancelButton =
                    nodeInfo.findDialogButton(arrayTextCancelButton, false) != null
                // Real custom dialog (has "Cancel"): click its "Force stop", finish
                if (foundCancelButton) {
                    val forceStopDialogButton =
                        nodeInfo.findDialogButton(arrayTextForceStopButton, false)
                    forceStopDialogButton?.let {
                        doPerformClick(it, "force stop dialog - force stop button")
                    }

                    // ignore everything and move to the next step
                    forceStopTries = 0
                    return PACKAGE_WAIT_NEXT_STEP
                }

                // Android 14+ (Compose Settings) emits stale recomposition /
                // content-change events between the "Force stop" click and the
                // real dialog window. Such an event is neither the dialog nor a
                // custom dialog => the dialog has not appeared yet. The wait flag
                // is one-shot, so consuming it here makes the real dialog be
                // ignored (forceStopTries already 0) and the package times out.
                // Re-arm and keep waiting instead; bounded by forceStopTries.
                // Gated to API 34+: classic Views (API <=33) deliver the dialog
                // as the next event, so they keep the original bail behavior.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    && forceStopTries-- > 0) {
                    forceStopWaitDialog = true
                    return PACKAGE_WAIT_DIALOG
                }

                // give up (API <=33, or tries exhausted), move to the next step
                forceStopTries = 0
                return PACKAGE_WAIT_NEXT_STEP
            }
        }

        when (foundForceStopDialog) {
            true -> {
                when {
                    // For Android N MR1 and early need to wait dialog
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.O -> {
                        // try refresh
                        while (!findForceStopDialogOkButton(nodeInfo)) {
                            nodeInfo.refresh()
                            delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)
                        }
                    }
                    else -> findForceStopDialogOkButton(nodeInfo)
                }

                // it's force stop dialog, nothing to do more, exit
                forceStopTries = 0
                return PACKAGE_WAIT_NEXT_STEP
            }
            false -> {
                when (findForceStopButton(nodeInfo)) {
                    false, null -> {
                        // nothing found, fall down to clear cache scenario
                        forceStopTries = 0
                        forceStopWaitDialog = false
                    }
                    true -> {
                        // found "Force stop" button, wait force stop dialog
                        forceStopTries = DEFAULT_FORCE_STOP_TRIES
                        forceStopWaitDialog = true
                        return PACKAGE_WAIT_DIALOG
                    }
                }
            }
        }

        // try again?
        forceStopTries--

        return null
    }

    private suspend fun processStorage(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        var recyclerViewNodeInfo: AccessibilityNodeInfo? = nodeInfo

        while (recyclerViewNodeInfo != null) {

            // first use "nodeInfo", then refreshed RecyclerView
            findStorageAndCacheMenu(recyclerViewNodeInfo)?.let { return it }

            // re-assign RecyclerView nodeInfo
            recyclerViewNodeInfo = nodeInfo.findRecyclerView() ?: break

            // scroll forward the RecyclerView to display the "Storage" menu
            if (doPerformScrollForward(recyclerViewNodeInfo, "RecycleView") != true)
                break

            delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)

            if (!recyclerViewNodeInfo.refresh())
                break

            delay(MIN_DELAY_PERFORM_CLICK_MS.toLong().milliseconds)

            if (BuildConfig.DEBUG) {
                Logger.d("===>>> recyclerView TREE BEGIN <<<===")
                recyclerViewNodeInfo.showTree(0, 0)
                Logger.d("===>>> recyclerView TREE END <<<===")
            }
        }

        return null
    }

    override suspend fun doClearCache(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        processForceStop(nodeInfo)?.let { return it }
        findClearCacheButton(nodeInfo)?.let { return it }
        processStorage(nodeInfo)?.let { return it }
        return null
    }

    override suspend fun doClearData(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        return when (findClearDataDialogTitle(nodeInfo)) {
            true ->
                when (findClearDataDialogDeleteButton(nodeInfo)) {
                    true -> PACKAGE_FINISH
                    false -> PACKAGE_FINISH_FAILED
                    null -> // fallback for old Android versions
                        when (findClearDataDialogOkButton(nodeInfo)) {
                            true -> PACKAGE_FINISH
                            false -> PACKAGE_FINISH_FAILED
                            null -> PACKAGE_FINISH_FAILED
                        }
                }
            else ->
                when (findClearDataButton(nodeInfo)) {
                    true -> PACKAGE_WAIT_NEXT_STEP
                    false -> PACKAGE_FINISH_FAILED
                    null -> processStorage(nodeInfo)
                }
        }
    }
}

// Hoisted out of the per-node finders: previously recompiled at every node of
// every subtree walk, on every accessibility event.
private val SETTINGS_BUTTON_ID_REGEX = "com.android.settings:id/.*button.*".toRegex()
private val DIALOG_BUTTON_ID_REGEX = "android:id/button.*".toRegex()

// Collapsed clear-cache / clear-data / force-stop finders (were byte-identical):
// locate a text/button node matching [arrayText] under [viewIdResourceName],
// return its clickable ancestor.
private fun AccessibilityNodeInfo.findClickableByText(
    arrayText: ArrayList<CharSequence>,
    viewIdResourceName: Regex): AccessibilityNodeInfo? =
    findNode { node ->
        node.takeIfMatches(
            findTextView = true,
            findButton = true,
            viewIdResourceName = viewIdResourceName,
            arrayText = arrayText,
        )?.findClickable()
    }

private fun AccessibilityNodeInfo.findStorageAndCacheMenu(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo? =
    findNode { node ->
        node.takeIfMatches(
            findTextView = true,
            findButton = true,
            viewIdResourceName = "android:id/title",
            arrayText = arrayText,
        )?.findClickable()
    }

private fun AccessibilityNodeInfo.findRecyclerView(): AccessibilityNodeInfo? =
    findNode { node ->
        node.takeIf { it.findByViewIdResourceName("com.android.settings:id/recycler_view") }
    }

private fun AccessibilityNodeInfo.findDialogButton(
    arrayText: ArrayList<CharSequence>, findTextView: Boolean = true): AccessibilityNodeInfo? =
    findNode { node ->
        node.takeIfMatches(
            findTextView = findTextView,
            findButton = true,
            viewIdResourceName = DIALOG_BUTTON_ID_REGEX,
            arrayText = arrayText,
        )?.findClickable()
    }

private fun AccessibilityNodeInfo.findDialogTitle(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo? =
    findNode { node ->
        node.takeIfMatches(
            findTextView = true,
            findButton = false,
            viewIdResourceName = "android:id/alertTitle",
            arrayText = arrayText,
        )
    }