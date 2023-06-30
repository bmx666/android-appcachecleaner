package com.github.bmx666.appcachecleaner.clearcache.scenario

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            nodeInfo.findStorageAndCacheMenuApi34(arrayTextStorageAndCacheMenu)?.let { return fn(it) }

        return false
    }

    override suspend fun doCacheClean(nodeInfo: AccessibilityNodeInfo) {
        if (findClearCacheButton(nodeInfo))
            return

        var recyclerViewNodeInfo: AccessibilityNodeInfo? = nodeInfo

        while (recyclerViewNodeInfo != null) {

            // first use "nodeInfo", then refreshed RecyclerView
            if (findStorageAndCacheMenu(recyclerViewNodeInfo))
                return

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

        stateMachine.setFinishCleanApp()
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
        nodeInfo.viewIdResourceName?.contentEquals("android:id/title") == true
                && arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun AccessibilityNodeInfo.findStorageAndCacheMenuApi34(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findStorageAndCacheMenuApi34(arrayText)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.className?.contentEquals("android.widget.TextView") == true
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