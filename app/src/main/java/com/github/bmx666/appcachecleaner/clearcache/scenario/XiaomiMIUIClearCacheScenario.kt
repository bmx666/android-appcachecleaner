package com.github.bmx666.appcachecleaner.clearcache.scenario

import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.clearcache.scenario.state.XiaomiMIUIStateMachine
import com.github.bmx666.appcachecleaner.util.getAllChild
import com.github.bmx666.appcachecleaner.util.lowercaseCompareText

internal class XiaomiMIUIClearCacheScenario: BaseClearCacheScenario() {

    override val stateMachine = XiaomiMIUIStateMachine()

    private suspend fun findClearDataButton(nodeInfo: AccessibilityNodeInfo): Boolean {
        nodeInfo.findMenuItemText(arrayTextClearDataButton)?.let { clearDataButton ->
            when (doPerformClick(clearDataButton, "Xiaomi MIUI - clear data button")) {
                // clear data button was found and it's enabled but perform click was failed
                false -> stateMachine.setInterrupted()
                // move to the next step
                else -> stateMachine.setOpenClearDataDialog()
            }
            return true
        }
        return false
    }

    private suspend fun findClearCacheButton(nodeInfo: AccessibilityNodeInfo): Boolean {
        nodeInfo.findMenuItemText(arrayTextClearCacheButton)?.let { clearCacheButton ->
            when (doPerformClick(clearCacheButton, "Xiaomi MIUI - clear cache button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> stateMachine.setInterrupted()
                // move to the next app
                else -> stateMachine.setOpenClearCacheDialog()
            }
            return true
        }
        return false
    }

    private suspend fun findClearDataDialogClearCacheButton(nodeInfo: AccessibilityNodeInfo): Boolean {
        nodeInfo.findDialogText(arrayTextClearCacheButton)?.let { clearCacheDialogButton ->
            when (doPerformClick(clearCacheDialogButton, "Xiaomi MIUI - clear data dialog - clear cache button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> stateMachine.setInterrupted()
                // move to the next app
                else -> stateMachine.setOpenClearCacheDialog()
            }
            return true
        }
        return false
    }

    private suspend fun findClearCacheDialogOkButton(nodeInfo: AccessibilityNodeInfo): Boolean {
        nodeInfo.findDialogButton(arrayTextOkButton)?.let { clearCacheDialogButton ->
            when (doPerformClick(clearCacheDialogButton, "Xiaomi MIUI - clear cache dialog - ok button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> stateMachine.setInterrupted()
                // move to the next app
                else -> stateMachine.setFinishCleanApp()
            }
            return true
        }
        return false
    }

    override suspend fun doCacheClean(nodeInfo: AccessibilityNodeInfo) {
        if (stateMachine.isOpenClearDataDialog()) {
            if (findClearDataDialogClearCacheButton(nodeInfo))
                return
        } else if (stateMachine.isOpenClearCacheDialog()) {
            if (findClearCacheDialogOkButton(nodeInfo))
                return
        } else {
            if (findClearCacheButton(nodeInfo))
                return
            if (findClearDataButton(nodeInfo))
                return
        }

        stateMachine.setFinishCleanApp()
    }

    override fun processState() {
        // find "Clear data" or "Clear cache" button and do perform click
        if (!stateMachine.waitState(maxWaitAppTimeoutMs.toLong()))
            stateMachine.setInterrupted()

        // 1. nothing found, sometimes it displays "Manage space" button
        // that mean - 0 bytes of cache and only clear user data available
        // 2. nothing is enabled, skip
        if (stateMachine.isFinishCleanApp())
            return

        // state not changes, something goes wrong...
        if (stateMachine.isInterrupted())
            return

        // find "Clear data" or "Clear cache" dialog and do perform click
        if (!stateMachine.waitState(maxWaitAppTimeoutMs.toLong()))
            stateMachine.setInterrupted()

        // 1. found "Clear data" dialog and "Clear cache" perform clicked
        // 2. found "Clear cache" dialog and "OK" perform clicked
        // 3. nothing is enabled, skip
        if (stateMachine.isFinishCleanApp())
            return

        // state not changes, something goes wrong...
        if (stateMachine.isInterrupted())
            return

        // "Clear cache" dialog and do perform click
        if (!stateMachine.waitState(maxWaitAppTimeoutMs.toLong()))
            stateMachine.setInterrupted()
    }
}

private fun AccessibilityNodeInfo.findMenuItemText(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findMenuItemText(arrayText)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.viewIdResourceName?.contentEquals("com.miui.securitycenter:id/action_menu_item_child_text") == true
                && arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
    }
}

private fun AccessibilityNodeInfo.findDialogText(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findDialogText(arrayText)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.viewIdResourceName?.matches("android:id/text.*".toRegex()) == true
                && arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
    }
}

private fun AccessibilityNodeInfo.findDialogButton(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findDialogButton(arrayText)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.viewIdResourceName?.matches("android:id/button.*".toRegex()) == true
                && arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
    }
}