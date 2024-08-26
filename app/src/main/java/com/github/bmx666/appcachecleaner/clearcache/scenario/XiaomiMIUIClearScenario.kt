package com.github.bmx666.appcachecleaner.clearcache.scenario

import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_FINISH_FAILED
import com.github.bmx666.appcachecleaner.const.Constant.CancellationJobMessage.Companion.PACKAGE_WAIT_NEXT_STEP
import com.github.bmx666.appcachecleaner.util.findClickable
import com.github.bmx666.appcachecleaner.util.getAllChild
import com.github.bmx666.appcachecleaner.util.takeIfMatches
import kotlinx.coroutines.CancellationException

/***
 * Steps to clean app cache:
 *
 * 1. open "App Info" settings for specific package
 * 2. if Accessibility event timeout has been expired, goto #7
 * 3. If "Clear Data Dialog" state:
 *    3.1. "Clear Cache" Menu Item" not found, goto #7
 *    3.2. "Clear Cache" Menu Item" found:
 *       3.2.1. if menu item is enabled, click it:
 *          3.2.1.1. if perform click was success, switched on "Clear Cache Dialog" and goto #2
 *          3.2.1.2. if perform click failed, goto #8
 *       3.2.2. if menu item is disabled, goto #7
 * 4. If "Clear Cache Dialog" state:
 *    4.1. "OK" button" not found, goto #7
 *    4.2. "OK" button" found:
 *       4.2.1. if button is enabled, click it:
 *          4.2.1.1. if perform click was success, goto #7
 *          4.2.1.2. if perform click failed, goto #8
 *       4.2.2. if button is disabled, goto #7
 * 5. find "clear cache button":
 *    NOTE: some Settings UI can have it on "App Info" settings
 *    5.1. "Clear Cache Button" not found, goto #6
 *    5.2. "Clear Cache Button" found:
 *       5.2.1. if button is enabled, click it:
 *          5.2.1.1. if perform click was success, switched on "Clear Cache Dialog" and goto #2
 *          5.2.1.2. if perform click failed, goto #8
 *       5.2.2. if button is disabled, goto #6
 * 6. find "Clear Data Button":
 *    6.1. "Clear Data Button" not found, goto #7
 *    6.2. "Clear Data Button" found:
 *       6.2.1. if menu is enabled, click it:
 *          6.2.1.1. if perform click was success, switched on "Clear Data Dialog" and goto #2
 *          6.2.1.2. if perform click failed, goto #8
 *       6.2.2. if menu was disabled, goto #7
 * 7. finish app clean process and move to the next
 * 8. interrupt clean process and halt
 ***/

internal class XiaomiMIUIClearScenario: BaseClearScenario() {

    private enum class State {
        INIT,
        OPEN_CLEAR_DATA_DIALOG,
        OPEN_CLEAR_CACHE_DIALOG,
    }

    private var state = State.INIT

    override fun resetInternalState() {
        state = State.INIT
    }

    private suspend fun findClearDataButton(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        nodeInfo.findMenuItemText(arrayTextClearDataButton)?.let { clearDataButton ->
            return when (doPerformClick(clearDataButton, "Xiaomi MIUI - clear data button")) {
                // clear data button was found and it's enabled but perform click was failed
                false -> {
                    state = State.INIT
                    PACKAGE_FINISH_FAILED
                }
                // move to the next step
                else -> {
                    state = State.OPEN_CLEAR_DATA_DIALOG
                    PACKAGE_WAIT_NEXT_STEP
                }
            }
        }
        return null
    }

    private suspend fun findClearCacheButton(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        nodeInfo.findMenuItemText(arrayTextClearCacheButton)?.let { clearCacheButton ->
            return when (doPerformClick(clearCacheButton, "Xiaomi MIUI - clear cache button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> {
                    state = State.INIT
                    PACKAGE_FINISH_FAILED
                }
                // move to the next step
                else -> {
                    state = State.OPEN_CLEAR_CACHE_DIALOG
                    PACKAGE_WAIT_NEXT_STEP
                }
            }
        }
        return null
    }

    private suspend fun findClearDataDialogClearCacheButton(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        nodeInfo.findDialogText(arrayTextClearCacheButton)?.let { clearCacheDialogButton ->
            return when (doPerformClick(clearCacheDialogButton, "Xiaomi MIUI - clear data dialog - clear cache button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> {
                    state = State.INIT
                    PACKAGE_FINISH_FAILED
                }
                // move to the next step
                else -> {
                    state = State.OPEN_CLEAR_CACHE_DIALOG
                    PACKAGE_WAIT_NEXT_STEP
                }
            }
        }
        return null
    }

    private suspend fun findClearCacheDialogOkButton(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        nodeInfo.findDialogButton(arrayTextOkButton)?.let { clearCacheDialogButton ->
            return when (doPerformClick(clearCacheDialogButton, "Xiaomi MIUI - clear cache dialog - ok button")) {
                // clean cache button was found and it's enabled but perform click was failed
                false -> {
                    state = State.INIT
                    PACKAGE_FINISH_FAILED
                }
                // move to the next app
                else -> {
                    state = State.INIT
                    PACKAGE_FINISH
                }
            }
        }
        return null
    }

    override suspend fun doCacheClean(nodeInfo: AccessibilityNodeInfo): CancellationException? {
        if (state == State.OPEN_CLEAR_DATA_DIALOG)
            findClearDataDialogClearCacheButton(nodeInfo)?.let { return it }
        else if (state == State.OPEN_CLEAR_CACHE_DIALOG)
            findClearCacheDialogOkButton(nodeInfo)?.let { return it }
        else {
            findClearCacheButton(nodeInfo)?.let { return it }
            findClearDataButton(nodeInfo)?.let { return it }
        }
        return null
    }
}

private fun AccessibilityNodeInfo.findMenuItemText(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findMenuItemText(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = false,
        findButton = false,
        viewIdResourceName = "com.miui.securitycenter:id/action_menu_item_child_text",
        arrayText = arrayText,
    )?.findClickable()
}

private fun AccessibilityNodeInfo.findDialogText(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findDialogText(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = false,
        findButton = false,
        viewIdResourceName = "android:id/text.*".toRegex(),
        arrayText = arrayText,
    )?.findClickable()
}

private fun AccessibilityNodeInfo.findDialogButton(
    arrayText: ArrayList<CharSequence>): AccessibilityNodeInfo?
{
    this.getAllChild().forEach { childNode ->
        childNode?.findDialogButton(arrayText)?.let { return it }
    }

    return this.takeIfMatches(
        findTextView = false,
        findButton = false,
        viewIdResourceName ="android:id/button.*".toRegex(),
        arrayText = arrayText,
    )?.findClickable()
}