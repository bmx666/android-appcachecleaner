package com.github.bmx666.appcachecleaner

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class AppCacheCleanerService : AccessibilityService() {

    private var mAccessibilityButtonController: AccessibilityButtonController? = null
    private var mAccessibilityButtonCallback: AccessibilityButtonCallback? = null
    private var mIsAccessibilityButtonAvailable: Boolean = false

    private fun compareText(nodeInfo: AccessibilityNodeInfo, text: CharSequence): Boolean {
        //Log.v(TAG, "search string $text")
        return nodeInfo.text?.toString()?.lowercase().contentEquals(text.toString().lowercase())
    }

    private fun findClickable(nodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        return when {
            nodeInfo == null -> null
            nodeInfo.isClickable -> nodeInfo
            else -> findClickable(nodeInfo.parent)
        }
    }

    private fun findClearCacheButton(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until nodeInfo.childCount) {
            if (nodeInfo.getChild(i) == null) continue
            val found = findClearCacheButton(nodeInfo.getChild(i))
            if (found != null) return found
        }

        return if (
            nodeInfo.viewIdResourceName?.startsWith("com.android.settings:id/button") == true
            && compareText(nodeInfo, getText(R.string.clear_cache_btn_text))
        ) findClickable(nodeInfo) else null
    }

    private fun findStorageAndCacheMenu(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until nodeInfo.childCount) {
            if (nodeInfo.getChild(i) == null) continue
            val found = findStorageAndCacheMenu(nodeInfo.getChild(i))
            if (found != null) return found
        }

        val resId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            R.string.storage_settings_for_app else R.string.storage_label

        return if (
            nodeInfo.viewIdResourceName?.contentEquals("android:id/title") == true
            && compareText(nodeInfo, getText(resId))
        ) findClickable(nodeInfo) else null
    }

    private fun findBackButton(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val actionBar = nodeInfo.findAccessibilityNodeInfosByViewId(
            "com.android.settings:id/action_bar").firstOrNull() ?: return null

        for (i in 0 until actionBar.childCount) {
            if (actionBar.getChild(i)?.className?.contentEquals("android.widget.ImageButton") == true)
                return actionBar.getChild(i)
        }
        return null
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
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mLocalReceiver, IntentFilter("disableSelf"))
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

    private fun showTree(level: Int, nodeInfo: AccessibilityNodeInfo) {
        Log.v(TAG, ">".repeat(level) + " " + nodeInfo.className
                + ":" + nodeInfo.text+ ":" + nodeInfo.viewIdResourceName)
        for (i in 0 until nodeInfo.childCount)
            showTree(level + 1, nodeInfo.getChild(i))
    }

    private fun goBack(nodeInfo: AccessibilityNodeInfo) {
        findBackButton(nodeInfo)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
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
            val storageAndCacheMenu = findStorageAndCacheMenu(nodeInfo)
            val clearCacheButton = findClearCacheButton(nodeInfo)

            if (storageAndCacheMenu != null) {
                if (storageAndCacheMenu.isEnabled) {
                    //Log.v(TAG, "found and click storage & cache")
                    storageAndCacheMenu.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    goBack(nodeInfo)
                    // notify main app go to another app
                    AppCacheCleanerActivity.waitAccessibility.open()
                }
            } else if (clearCacheButton != null) {
                if (clearCacheButton.isEnabled) {
                    //Log.v(TAG, "found and click clean cache button")
                    clearCacheButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                goBack(nodeInfo)
            } else {
                goBack(nodeInfo)
                // notify main app go to another app
                AppCacheCleanerActivity.waitAccessibility.open()
            }
        }
    }

    override fun onInterrupt() {}

    companion object {
        private val TAG = AppCacheCleanerService::class.java.simpleName
    }
}
