package com.github.bmx666.appcachecleaner.clearcache.scenario

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.clearcache.scenario.state.DefaultStateMachine
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.util.getAllChild
import com.github.bmx666.appcachecleaner.util.lowercaseCompareText
import com.github.bmx666.appcachecleaner.util.showTree
import kotlinx.coroutines.delay

internal class DefaultClearCacheScenario: BaseClearCacheScenario() {

    override val stateMachine = DefaultStateMachine()

    private suspend fun findClearCacheButton(nodeInfo: AccessibilityNodeInfo): Boolean {
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
                        stateMachine.setInterrupted()
                        return true
                    }
                    Logger.d("===>>> findClearCacheButton: loop: refresh, END <<<===")

                    delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
                }
            }

            when (doPerformClick(clearCacheButton, "clean cache button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> stateMachine.setInterrupted()
                // move to the next app
                else -> stateMachine.setFinishCleanApp()
            }
            return true
        }
        return false
    }

    private suspend fun findStorageAndCacheMenu(nodeInfo: AccessibilityNodeInfo): Boolean {
        suspend fun fn(storageAndCacheMenu: AccessibilityNodeInfo): Boolean {
            when (doPerformClick(storageAndCacheMenu, "storage & cache button")) {
                // move to the next app
                null -> stateMachine.setFinishCleanApp()
                // storage & cache button was found and it's enabled but perform click was failed
                false -> stateMachine.setInterrupted()
                // open App Storage Activity
                true -> stateMachine.setOpenStorageInfo()
            }
            return true
        }

        nodeInfo.findStorageAndCacheMenu(arrayTextStorageAndCacheMenu)?.let { return fn(it) }

        return false
    }

    override suspend fun doCacheClean(nodeInfo: AccessibilityNodeInfo) {
        Logger.d("===>>> doCacheClean BEGIN <<<===")
        if (cacheClean(nodeInfo)) {
            Logger.d("===>>> doCacheClean: cacheClean END <<<===")
            return
        }

        if (!stateMachine.isOpenStorageInfo()) {
            if (!stateMachine.isInterrupted())
                stateMachine.setFinishCleanApp()
            Logger.d("===>>> doCacheClean: !stateMachine.isOpenStorageInfo END <<<===")
            return
        }

        Logger.d("===>>> nodeInfo refresh BEGIN <<<===")
        nodeInfo.refresh()
        Logger.d("===>>> nodeInfo.refresh END <<<===")
        Logger.d("===>>> doCacheClean END <<<===")
    }

    private suspend fun cacheClean(nodeInfo: AccessibilityNodeInfo): Boolean {
        if (findClearCacheButton(nodeInfo))
            return true

        var recyclerViewNodeInfo: AccessibilityNodeInfo? = nodeInfo

        while (recyclerViewNodeInfo != null) {

            // first use "nodeInfo", then refreshed RecyclerView
            if (findStorageAndCacheMenu(recyclerViewNodeInfo))
                return true

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
                recyclerViewNodeInfo.showTree(0)
                Logger.d("===>>> recyclerView TREE END <<<===")
            }
        }

        return false
    }

    override fun processState() {
        // find "Storage & cache" or "Clean cache" and do perform click
        if (!stateMachine.waitState(maxWaitAppTimeoutMs.toLong()))
            stateMachine.setInterrupted()

        // found "clear cache" and perform clicked
        // OR "Storage & cache" is disabled
        if (stateMachine.isFinishCleanApp())
            return

        // state not changes, something goes wrong...
        if (stateMachine.isInterrupted())
            return

        // find "Clean cache" and do perform click
        if (!stateMachine.waitState(maxWaitAppTimeoutMs.toLong()))
            stateMachine.setInterrupted()

        // wait before to move to the next app
        if (delayForNextAppTimeoutMs > 0) {
            stateMachine.setDelayForNextApp()
            stateMachine.waitState(delayForNextAppTimeoutMs.toLong())
        }
    }
}

private fun AccessibilityNodeInfo.findClearCacheButton(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findClearCacheButton(arrayText)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.viewIdResourceName?.matches("com.android.settings:id/.*button.*".toRegex()) == true
                && arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
    }
}

private fun AccessibilityNodeInfo.findStorageAndCacheMenu(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findStorageAndCacheMenu(arrayText)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            nodeInfo.className?.contentEquals("android.widget.TextView") == true
                    && arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
        else
            nodeInfo.viewIdResourceName?.contentEquals("android:id/title") == true
                    && arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
    }
}

private fun AccessibilityNodeInfo.findRecyclerView(): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findRecyclerView()?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.viewIdResourceName?.contentEquals("com.android.settings:id/recycler_view") == true
    }
}