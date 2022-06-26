package com.github.bmx666.appcachecleaner

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.util.findNestedChildByClassName
import com.github.bmx666.appcachecleaner.util.getAllChild
import com.github.bmx666.appcachecleaner.util.lowercaseCompareText
import com.github.bmx666.appcachecleaner.util.performClick


class AppCacheCleanerService : AccessibilityService() {

    private var mAccessibilityButtonController: AccessibilityButtonController? = null
    private var mAccessibilityButtonCallback: AccessibilityButtonCallback? = null
    private var mIsAccessibilityButtonAvailable: Boolean = false

    private fun findClearCacheButton(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        nodeInfo.getAllChild().forEach { childNode ->
            findClearCacheButton(childNode)?.let { return it }
        }

        return nodeInfo.takeIf {
            nodeInfo.viewIdResourceName?.matches("com.android.settings:id/.*button.*".toRegex()) == true
                    && nodeInfo.lowercaseCompareText(textClearCacheButton)
        }
    }

    private fun findStorageAndCacheMenu(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        nodeInfo.getAllChild().forEach { childNode ->
            findStorageAndCacheMenu(childNode)?.let { return it }
        }

        return nodeInfo.takeIf {
            nodeInfo.viewIdResourceName?.contentEquals("android:id/title") == true
                    && nodeInfo.lowercaseCompareText(textStorageAndCacheMenu)
        }
    }

    private fun findBackButton(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val actionBar = nodeInfo.findAccessibilityNodeInfosByViewId(
            "com.android.settings:id/action_bar").firstOrNull()
            ?: nodeInfo.findAccessibilityNodeInfosByViewId(
                "android:id/action_bar").firstOrNull()
            ?: return null

        // WORKAROUND: on some smartphones ActionBar Back button has ID "up"
        actionBar.findAccessibilityNodeInfosByViewId(
            "android:id/up").firstOrNull()?.let { return it }

        return actionBar.findNestedChildByClassName(
            arrayOf("android.widget.ImageButton", "android.widget.ImageView"))
    }

    private val mLocalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) unregisterButton()
            disableSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        updateLocaleText()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mLocalReceiver, IntentFilter("disableSelf"))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLocaleText()
    }

    private fun updateLocaleText() {
        textClearCacheButton = getText(R.string.clear_cache_btn_text)

        textStorageAndCacheMenu =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                getText(R.string.storage_settings_for_app)
            else
                getText(R.string.storage_label)
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            unregisterButton()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver)
        super.onDestroy()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            registerButton()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerButton() {
        mAccessibilityButtonController = accessibilityButtonController

        // Accessibility Button is available on Android 30 and early
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (mAccessibilityButtonController?.isAccessibilityButtonAvailable != true)
                return
        }

        mAccessibilityButtonCallback =
            object : AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    if (AppCacheCleanerActivity.cleanCacheFinished.get()) return
                    Log.d(TAG, "Accessibility button pressed!")
                    AppCacheCleanerActivity.cleanCacheInterrupt.set(true)
                    AppCacheCleanerActivity.waitAccessibility.open()
                }

                override fun onAvailabilityChanged(
                    controller: AccessibilityButtonController,
                    available: Boolean
                ) {
                    if (controller == mAccessibilityButtonController) {
                        Log.d(TAG, "Accessibility button available = $available")
                        mIsAccessibilityButtonAvailable = available
                    }
                }
            }

        mAccessibilityButtonCallback?.also {
            mAccessibilityButtonController?.registerAccessibilityButtonCallback(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun unregisterButton() {
        mAccessibilityButtonCallback?.let {
            mAccessibilityButtonController?.unregisterAccessibilityButtonCallback(it)
        }
    }

    private fun showTree(level: Int, nodeInfo: AccessibilityNodeInfo?) {
        if (nodeInfo == null) return
        Log.v(TAG, ">".repeat(level) + " " + nodeInfo.className
                + ":" + nodeInfo.text+ ":" + nodeInfo.viewIdResourceName)
        nodeInfo.getAllChild().forEach { childNode ->
            showTree(level + 1, childNode)
        }
    }

    private fun goBack(nodeInfo: AccessibilityNodeInfo) {
        findBackButton(nodeInfo)?.performClick()
        AppCacheCleanerActivity.cleanAppCacheFinished.set(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (AppCacheCleanerActivity.cleanCacheFinished.get()) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return

        val nodeInfo = event.source ?: return
        // showTree(0, nodeInfo)

        if (AppCacheCleanerActivity.cleanAppCacheFinished.get()) {
            goBack(nodeInfo)
            // notify main app go to another app
            AppCacheCleanerActivity.waitAccessibility.open()
        } else {

            findClearCacheButton(nodeInfo)?.let { clearCacheButton ->
                if (clearCacheButton.isEnabled) {
                    //Log.v(TAG, "found and click clean cache button")
                    clearCacheButton.performClick()
                }
                goBack(nodeInfo)
                return
            }

            findStorageAndCacheMenu(nodeInfo)?.let { storageAndCacheMenu ->
                if (storageAndCacheMenu.isEnabled) {
                    //Log.v(TAG, "found and click storage & cache")
                    storageAndCacheMenu.performClick()
                } else {
                    goBack(nodeInfo)
                    // notify main app go to another app
                    AppCacheCleanerActivity.waitAccessibility.open()
                }
                return
            }

            goBack(nodeInfo)
            // notify main app go to another app
            AppCacheCleanerActivity.waitAccessibility.open()
        }
    }

    override fun onInterrupt() {}

    companion object {
        private val TAG = AppCacheCleanerService::class.java.simpleName

        private lateinit var textClearCacheButton: CharSequence
        private lateinit var textStorageAndCacheMenu: CharSequence
    }
}
