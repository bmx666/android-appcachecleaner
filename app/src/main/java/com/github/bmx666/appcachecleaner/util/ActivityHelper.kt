package com.github.bmx666.appcachecleaner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.github.bmx666.appcachecleaner.service.CacheCleanerTileService

class ActivityHelper {

    companion object {

        @JvmStatic
        fun startApplicationDetailsActivity(context: Context?, packageName: String?) {
            // everything is possible...
            if (packageName == null || packageName.trim().isEmpty()) return
            try {
                context?.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                        if (context is CacheCleanerTileService)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = Uri.parse("package:$packageName")
                    }
                )
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun showAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            if (context is CacheCleanerTileService)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        @JvmStatic
        fun showUsageAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            if (context is CacheCleanerTileService)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        @JvmStatic
        fun returnBackToMainActivity(context: Context, intent: Intent) {
            context.startActivity(
                intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                }
            )
        }

        @JvmStatic
        fun navigateToHomeScreen(context: Context) {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}