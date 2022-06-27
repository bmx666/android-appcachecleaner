package com.github.bmx666.appcachecleaner

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.log.TimberFileTree
import com.github.bmx666.appcachecleaner.util.findNestedChildByClassName
import com.github.bmx666.appcachecleaner.util.getAllChild
import com.github.bmx666.appcachecleaner.util.lowercaseCompareText
import com.github.bmx666.appcachecleaner.util.performClick
import timber.log.Timber
import java.io.File


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
                    && arrayTextClearCacheButton.any { text -> nodeInfo.lowercaseCompareText(text) }
        }
    }

    private fun findStorageAndCacheMenu(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        nodeInfo.getAllChild().forEach { childNode ->
            findStorageAndCacheMenu(childNode)?.let { return it }
        }

        return nodeInfo.takeIf {
            nodeInfo.viewIdResourceName?.contentEquals("android:id/title") == true
                    && arrayTextStorageAndCacheMenu.any { text -> nodeInfo.lowercaseCompareText(text) }
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
            when (intent?.action) {
                "disableSelf" -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) unregisterButton()
                    disableSelf()
                }
                "addExtraSearchText" -> {
                    updateLocaleText(
                        intent.getStringExtra("clear_cache"),
                        intent.getStringExtra("storage"))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG)
            createLogFile()
        updateLocaleText(null, null)
        val intentFilter = IntentFilter()
        intentFilter.addAction("disableSelf")
        intentFilter.addAction("addExtraSearchText")
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mLocalReceiver, intentFilter)
    }

    private fun updateLocaleText(clearCacheText: CharSequence?, storageText: CharSequence?) {
        arrayTextClearCacheButton.clear()
        clearCacheText?.let { arrayTextClearCacheButton.add(it) }
        arrayTextClearCacheButton.add(getText(R.string.clear_cache_btn_text))

        arrayTextStorageAndCacheMenu.clear()
        storageText?.let { arrayTextStorageAndCacheMenu.add(it) }
        arrayTextStorageAndCacheMenu.add(getText(R.string.storage_settings_for_app))
        arrayTextStorageAndCacheMenu.add(getText(R.string.storage_label))
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG)
            deleteLogFile()
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
                    Timber.d("Accessibility button pressed!")
                    AppCacheCleanerActivity.cleanCacheInterrupt.set(true)
                    AppCacheCleanerActivity.waitAccessibility.open()
                }

                override fun onAvailabilityChanged(
                    controller: AccessibilityButtonController,
                    available: Boolean
                ) {
                    if (controller == mAccessibilityButtonController) {
                        Timber.d("Accessibility button available = $available")
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
        Timber.d(">".repeat(level) + " " + nodeInfo.className
                + ":" + nodeInfo.text+ ":" + nodeInfo.viewIdResourceName)
        nodeInfo.getAllChild().forEach { childNode ->
            showTree(level + 1, childNode)
        }
    }

    private fun goBack(nodeInfo: AccessibilityNodeInfo) {
        findBackButton(nodeInfo)?.let { backButton ->
            Timber.d("found back button")
            when (backButton.performClick()) {
                true  -> Timber.d("perform action click on back button")
                false -> Timber.e("no perform action click on back button")
                else  -> Timber.e("not found clickable view for back button")
            }
        }
        AppCacheCleanerActivity.cleanAppCacheFinished.set(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (AppCacheCleanerActivity.cleanCacheFinished.get()) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return

        val nodeInfo = event.source ?: return

        if (BuildConfig.DEBUG) {
            Timber.d("===>>> TREE BEGIN <<<===")
            showTree(0, nodeInfo)
            Timber.d("===>>> TREE END <<<===")
        }

        if (AppCacheCleanerActivity.cleanAppCacheFinished.get()) {
            goBack(nodeInfo)
            // notify main app go to another app
            AppCacheCleanerActivity.waitAccessibility.open()
        } else {

            findClearCacheButton(nodeInfo)?.let { clearCacheButton ->
                Timber.d("found clean cache button")
                if (clearCacheButton.isEnabled) {
                    Timber.d("clean cache button is enabled")
                    when (clearCacheButton.performClick()) {
                        true  -> Timber.d("perform action click on clean cache button")
                        false -> Timber.e("no perform action click on clean cache button")
                        else  -> Timber.e("not found clickable view for clean cache button")
                    }
                }
                goBack(nodeInfo)
                return
            }

            findStorageAndCacheMenu(nodeInfo)?.let { storageAndCacheMenu ->
                Timber.d("found storage & cache button")
                if (storageAndCacheMenu.isEnabled) {
                    Timber.d("storage & cache button is enabled")
                    when (storageAndCacheMenu.performClick()) {
                        true  -> Timber.d("perform action click on storage & cache button")
                        false -> Timber.e("no perform action click on storage & cache button")
                        else  -> Timber.e("not found clickable view for storage & cache button")
                    }
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

    private fun createLogFile() {
        val logFile = File(cacheDir.absolutePath + "/log.txt")
        // force clean previous log
        logFile.writeText("")
        Timber.plant(TimberFileTree(logFile))
    }

    private fun deleteLogFile() {
        val logFile = File(cacheDir.absolutePath + "/log.txt")
        logFile.delete()
    }

    companion object {
        private val TAG = AppCacheCleanerService::class.java.simpleName

        private var arrayTextClearCacheButton = ArrayList<CharSequence>()
        private var arrayTextStorageAndCacheMenu = ArrayList<CharSequence>()
    }
}
