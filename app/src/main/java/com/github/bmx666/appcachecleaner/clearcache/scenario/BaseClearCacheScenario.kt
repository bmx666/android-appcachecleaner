package com.github.bmx666.appcachecleaner.clearcache.scenario

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_PERFORM_CLICK_COUNT_TRIES
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_CLEAR_CACHE_BUTTON_MS
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.util.clamp
import com.github.bmx666.appcachecleaner.util.performClick
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal abstract class BaseClearCacheScenario {

    protected val arrayTextClearCacheButton = ArrayList<CharSequence>()
    protected val arrayTextClearDataButton = ArrayList<CharSequence>()
    protected val arrayTextStorageAndCacheMenu = ArrayList<CharSequence>()
    protected val arrayTextOkButton = ArrayList<CharSequence>()

    internal var delayForNextAppTimeoutMs:
        Int = DEFAULT_DELAY_FOR_NEXT_APP_MS
        set(timeoutMs) {
            field = clamp(
                timeoutMs,
                MIN_DELAY_FOR_NEXT_APP_MS,
                MAX_DELAY_FOR_NEXT_APP_MS
            )
        }

    internal var maxWaitAppTimeoutMs:
        Int = DEFAULT_WAIT_APP_PERFORM_CLICK_MS
        set(timeoutMs) {
            field = timeoutMs.coerceAtLeast(MIN_WAIT_APP_PERFORM_CLICK_MS)
            maxPerformClickCountTries = (field - MIN_DELAY_PERFORM_CLICK_MS) / MIN_DELAY_PERFORM_CLICK_MS
        }

    internal var maxWaitClearCacheButtonTimeoutMs:
        Int = DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS
        set(timeoutMs) {
            field = clamp(
                timeoutMs,
                MIN_WAIT_CLEAR_CACHE_BUTTON_MS,
                maxWaitAppTimeoutMs - 1000
            )
        }

    private var maxPerformClickCountTries:
        Int = DEFAULT_PERFORM_CLICK_COUNT_TRIES

    internal var maxWaitAccessibilityEventMs:
        Int = DEFAULT_WAIT_ACCESSIBILITY_EVENT_MS
        set(timeoutMs) {
            field = clamp(
                timeoutMs,
                MIN_WAIT_ACCESSIBILITY_EVENT_MS,
                MAX_WAIT_ACCESSIBILITY_EVENT_MS
            )
        }

    internal var goBackAfterApps:
            Int = DEFAULT_GO_BACK_AFTER_APPS
        set(timeoutMs) {
            field = clamp(
                timeoutMs,
                MIN_GO_BACK_AFTER_APPS,
                MAX_GO_BACK_AFTER_APPS
            )
        }

    abstract fun resetInternalState()
    abstract suspend fun doCacheClean(nodeInfo: AccessibilityNodeInfo): CancellationException?

    fun setExtraSearchText(clearCacheTextList: ArrayList<CharSequence>,
                           clearDataTextList: ArrayList<CharSequence>,
                           storageTextList: ArrayList<CharSequence>,
                           okTextList: ArrayList<CharSequence>) {
        arrayTextClearCacheButton.clear()
        arrayTextClearCacheButton.addAll(clearCacheTextList)

        arrayTextClearDataButton.clear()
        arrayTextClearDataButton.addAll(clearDataTextList)

        arrayTextStorageAndCacheMenu.clear()
        arrayTextStorageAndCacheMenu.addAll(storageTextList)

        arrayTextOkButton.clear()
        arrayTextOkButton.addAll(okTextList)
    }

    protected suspend fun doPerformClick(nodeInfo: AccessibilityNodeInfo,
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

                if (tries++ >= maxPerformClickCountTries)
                    break

                delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
            } while (result != true)

            return (result == true)
        }

        return null
    }

    protected fun doPerformScrollForward(nodeInfo: AccessibilityNodeInfo,
                                       debugText: String): Boolean?
    {
        Logger.d("found $debugText")
        if (nodeInfo.isEnabled) {
            Logger.d("$debugText is enabled")

            val result = nodeInfo.performAction(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id,
                Bundle()
            )
            when (result) {
                true -> Logger.d("perform action scroll forward on $debugText")
                false -> Logger.e("no perform action scroll forward on $debugText")
            }

            return result
        }

        return null
    }
}