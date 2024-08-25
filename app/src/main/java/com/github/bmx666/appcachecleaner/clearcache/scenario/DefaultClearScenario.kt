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
import com.github.bmx666.appcachecleaner.util.getAllChild
import com.github.bmx666.appcachecleaner.util.showTree
import com.github.bmx666.appcachecleaner.util.takeIfMatches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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
        nodeInfo.findClearCacheButton(arrayTextClearCacheButton)?.let { clearCacheButton ->

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

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
                }
            }

            return when (doPerformClick(clearCacheButton, "clean cache button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> {
                    if (!nodeInfo.refresh()) {
                        Logger.w("clearCacheButton (no perform click): failed to refresh parent node")
                        return PACKAGE_FINISH_FAILED
                    }

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
                    return findClearCacheButton(nodeInfo)
                }
                true -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())

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

                    return PACKAGE_FINISH
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
                // storage & cache button was found and it's enabled but perform click was failed
                false -> {
                    if (!nodeInfo.refresh())
                        return PACKAGE_FINISH_FAILED

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
                    return findStorageAndCacheMenu(nodeInfo)
                }
            }
        }

        nodeInfo.findStorageAndCacheMenu(arrayTextStorageAndCacheMenu)?.let { return fn(it) }

        return null
    }

    private suspend fun findForceStopButton(nodeInfo: AccessibilityNodeInfo): Boolean? {
        nodeInfo.findForceStopButton(arrayTextForceStopButton)?.let { forceStopButton ->

            return when (doPerformClick(forceStopButton, "force stop button")) {
                // force stop button was found and it's enabled but perform click was failed
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
        nodeInfo.findClearDataButton(arrayTextClearDataButton)?.let { clearDataButton ->
            when (doPerformClick(clearDataButton, "clear data button")) {
                // clear data button was found and it's enabled but perform click was failed
                false -> {
                    if (!nodeInfo.refresh()) {
                        Logger.w("clearDataButton (no perform click): failed to refresh parent node")
                        return false
                    }

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
                    return findClearDataButton(nodeInfo)
                }
                true -> {
                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
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

        if (forceStopWaitDialog) {
            // force disable wait dialog to avoid misbehavior
            forceStopWaitDialog = false

            // For Android N MR1 and early need to wait dialog
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                // limit tries by app timeout
                var tries = maxWaitAppTimeoutMs / 250

                while (tries-- > 0) {
                    nodeInfo.refresh()
                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())

                    if (BuildConfig.DEBUG) {
                        Logger.d("===>>> FORCE STOP Dialog TREE BEGIN <<<===")
                        nodeInfo.showTree(0, 0)
                        Logger.d("===>>> FORCE STOP Dialog TREE END <<<===")
                    }

                    if (findForceStopDialogTitle(nodeInfo))
                        break
                }
            }
        }

        when (findForceStopDialogTitle(nodeInfo)) {
            true -> {
                when {
                    // For Android N MR1 and early need to wait dialog
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.O -> {
                        // try refresh
                        while (!findForceStopDialogOkButton(nodeInfo)) {
                            nodeInfo.refresh()
                            delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
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

            delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())

            if (!recyclerViewNodeInfo.refresh())
                break

            delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())

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

private fun AccessibilityNodeInfo.findClearCacheButton(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findClearCacheButton(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = true,
        findButton = true,
        viewIdResourceName = "com.android.settings:id/.*button.*".toRegex(),
        arrayText = arrayText,
    )?.findClickable()
}

private fun AccessibilityNodeInfo.findClearDataButton(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findClearDataButton(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = true,
        findButton = true,
        viewIdResourceName = "com.android.settings:id/.*button.*".toRegex(),
        arrayText = arrayText,
    )?.findClickable()
}

private fun AccessibilityNodeInfo.findForceStopButton(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findForceStopButton(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = true,
        findButton = true,
        viewIdResourceName = "com.android.settings:id/.*button.*".toRegex(),
        arrayText = arrayText,
    )?.findClickable()
}

private fun AccessibilityNodeInfo.findStorageAndCacheMenu(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findStorageAndCacheMenu(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = true,
        findButton = true,
        viewIdResourceName = "android:id/title",
        arrayText = arrayText,
    )?.findClickable()
}

private fun AccessibilityNodeInfo.findRecyclerView(): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findRecyclerView()?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.findByViewIdResourceName("com.android.settings:id/recycler_view")
    }
}

private fun AccessibilityNodeInfo.findDialogButton(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findDialogButton(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = true,
        findButton = true,
        viewIdResourceName = "android:id/button.*".toRegex(),
        arrayText = arrayText,
    )?.findClickable()
}

private fun AccessibilityNodeInfo.findDialogTitle(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findDialogTitle(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = true,
        findButton = false,
        viewIdResourceName = "android:id/alertTitle",
        arrayText = arrayText,
    )
}