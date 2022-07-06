package com.github.bmx666.appcachecleaner.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.github.bmx666.appcachecleaner.AppCacheCleanerService

class PermissionChecker {

    companion object {
        // method to check is the user has permitted the accessibility permission
        // if not then prompt user to the system's Settings activity
        @JvmStatic
        fun checkAccessibilityPermission(context: Context): Boolean {
            try {
                val accessibilityEnabled =
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED)

                if (accessibilityEnabled != 1) return false

                val accessibilityServiceName = context.packageName + "/" +
                        AppCacheCleanerService::class.java.name

                val enabledServices =
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

                val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
                stringColonSplitter.setString(enabledServices)
                while (stringColonSplitter.hasNext()) {
                    if (accessibilityServiceName.contentEquals(stringColonSplitter.next()))
                        return true
                }

                return false
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }

            return false
        }

        @JvmStatic
        fun checkUsageStatsPermission(context: Context): Boolean {
            try {
                val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
                val appOpsManager = context.getSystemService(AppCompatActivity.APP_OPS_SERVICE) as AppOpsManager
                val mode =
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                        appOpsManager.checkOpNoThrow(
                            AppOpsManager.OPSTR_GET_USAGE_STATS,
                            applicationInfo.uid, applicationInfo.packageName)
                    else
                        appOpsManager.unsafeCheckOpNoThrow(
                            AppOpsManager.OPSTR_GET_USAGE_STATS,
                            applicationInfo.uid, applicationInfo.packageName)

                return mode == AppOpsManager.MODE_ALLOWED
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            return false
        }

        @JvmStatic
        fun checkAllRequiredPermissions(context: Context): Boolean {
            return checkAccessibilityPermission(context) and
                    checkUsageStatsPermission(context)
        }
    }
}