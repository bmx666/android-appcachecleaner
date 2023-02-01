package com.github.bmx666.appcachecleaner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

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
            context.startActivity(intent)
        }

        @JvmStatic
        fun showUsageAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
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
    }
}