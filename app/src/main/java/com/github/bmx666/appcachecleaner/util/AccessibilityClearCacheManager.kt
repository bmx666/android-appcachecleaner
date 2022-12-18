package com.github.bmx666.appcachecleaner.util

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1


class AccessibilityClearCacheManager {

    fun setArrayTextClearCacheButton(array: ArrayList<CharSequence>) {
        arrayTextClearCacheButton.clear()
        arrayTextClearCacheButton.addAll(array)
    }

    fun setArrayTextStorageAndCacheMenu(array: ArrayList<CharSequence>) {
        arrayTextStorageAndCacheMenu.clear()
        arrayTextStorageAndCacheMenu.addAll(array)
    }

    private fun showTree(level: Int, nodeInfo: AccessibilityNodeInfo?) {
        if (nodeInfo == null) return
        Logger.d(">".repeat(level) + " " + nodeInfo.className
                + ":" + nodeInfo.text+ ":" + nodeInfo.viewIdResourceName)
        nodeInfo.getAllChild().forEach { childNode ->
            showTree(level + 1, childNode)
        }
    }

    suspend fun clearCacheApp(pkgList: ArrayList<String>, openAppInfo: KFunction1<String, Unit>): Boolean {

        stateMachine.init()

        for (pkg in pkgList) {
            // everything is possible...
            if (pkg.trim().isEmpty()) continue

            // state not changes, something goes wrong...
            if (stateMachine.isInterrupted()) break

            stateMachine.setOpenAppInfo()
            openAppInfo(pkg)

            // state not changes, something goes wrong...
            if (stateMachine.isInterrupted()) break

            // find "Storage & cache" or "Clean cache" and do perform click
            if (!stateMachine.waitState(MAX_WAIT_APP_PERFORM_CLICK_MS))
                stateMachine.setInterrupted()

            // found "clear cache" and perform clicked
            // OR "Storage & cache" is disabled
            if (stateMachine.isFinishCleanApp()) continue

            // state not changes, something goes wrong...
            if (stateMachine.isInterrupted()) break

            // find "Clean cache" and do perform click
            if (!stateMachine.waitState(MAX_WAIT_APP_PERFORM_CLICK_MS))
                stateMachine.setInterrupted()

            // state not changes, something goes wrong...
            if (stateMachine.isInterrupted()) break
        }

        val interrupted = stateMachine.isInterrupted()
        stateMachine.init()

        return interrupted
    }

    private suspend fun doPerformClick(nodeInfo: AccessibilityNodeInfo,
                                       debugText: String): Boolean?
    {
        Logger.d("found $debugText")
        if (nodeInfo.isEnabled) {
            Logger.d("$debugText is enabled")

            var result: Boolean?
            var tries: Long = 0
            do {
                result = nodeInfo.performClick()
                when (result) {
                    true -> Logger.d("perform action click on $debugText")
                    false -> Logger.e("no perform action click on $debugText")
                    else -> Logger.e("not found clickable view for $debugText")
                }

                if (result == true)
                    break

                if (tries++ >= MAX_COUNT_TRIES)
                    break

                delay(MIN_DELAY_PERFORM_CLICK_MS + tries * MIN_DELAY_PERFORM_CLICK_MS)
            } while (result != true)

            return (result == true)
        }

        return null
    }

    fun checkEvent(event: AccessibilityEvent) {

        if (stateMachine.isDone()) return

        if (event.source == null) {
            stateMachine.setFinishCleanApp()
            return
        }

        val nodeInfo = event.source!!

        if (BuildConfig.DEBUG) {
            Logger.d("===>>> TREE BEGIN <<<===")
            showTree(0, nodeInfo)
            Logger.d("===>>> TREE END <<<===")
        }

        nodeInfo.findClearCacheButton(arrayTextClearCacheButton)?.let { clearCacheButton ->
            CoroutineScope(Dispatchers.IO).launch {
                when (doPerformClick(clearCacheButton, "clean cache button")) {
                    // clean cache button was found and it's enabled but perform click was failed
                    false -> stateMachine.setInterrupted()
                    else -> {}
                }
                // move to the next app
                stateMachine.setFinishCleanApp()
            }
            return
        }

        nodeInfo.findStorageAndCacheMenu(arrayTextStorageAndCacheMenu)?.let { storageAndCacheMenu ->
            CoroutineScope(Dispatchers.IO).launch {
                when (doPerformClick(storageAndCacheMenu, "storage & cache button")) {
                    // move to the next app
                    null -> stateMachine.setFinishCleanApp()
                    // storage & cache button was found and it's enabled but perform click was failed
                    false -> stateMachine.setInterrupted()
                    // open App Storage Activity
                    true -> stateMachine.setStorageInfo()
                }
            }
            return
        }

        stateMachine.setFinishCleanApp()
    }

    fun clickAccessibilityButton() {
        if (stateMachine.isDone()) return
        Logger.d("Accessibility button was pressed!")
        stateMachine.setInterrupted()
    }

    companion object {
        private const val MAX_COUNT_TRIES: Long = 16
        private const val MIN_DELAY_PERFORM_CLICK_MS: Long = 250
        private const val MAX_WAIT_APP_PERFORM_CLICK_MS: Long =
            MAX_COUNT_TRIES / 2 * (2 * MIN_DELAY_PERFORM_CLICK_MS + (MAX_COUNT_TRIES - 1) * MIN_DELAY_PERFORM_CLICK_MS)


        private val arrayTextClearCacheButton = ArrayList<CharSequence>()
        private val arrayTextStorageAndCacheMenu = ArrayList<CharSequence>()

        private val stateMachine = CleanCacheStateMachine()
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