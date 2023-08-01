package com.github.bmx666.appcachecleaner.ui.dialog

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.util.ActivityHelper
import com.github.bmx666.appcachecleaner.util.PermissionChecker

class PermissionDialogBuilder {
    companion object {
        @JvmStatic
        fun buildAccessibilityPermissionDialog(context: Context) {
            AlertDialogBuilder(context)
                .setTitle(R.string.text_enable_accessibility_title)
                .setMessage(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        R.string.text_enable_accessibility_message_api33
                    else
                        R.string.text_enable_accessibility_message)
                .setPositiveButton(R.string.allow) { _, _ ->
                    ActivityHelper.showAccessibilitySettings(context)
                }
                .setNegativeButton(R.string.deny) { _, _ ->
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildUsageStatsPermissionDialog(context: Context) {
            AlertDialogBuilder(context)
                .setTitle(R.string.text_enable_usage_stats_title)
                .setMessage(R.string.text_enable_usage_stats_message)
                .setPositiveButton(R.string.allow) { _, _ ->
                    ActivityHelper.showUsageAccessSettings(context)
                }
                .setNegativeButton(R.string.deny) { _, _ ->
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildWriteExternalStoragePermissionDialog(context: Context,
            requestPermissionLauncher: ActivityResultLauncher<String>) {
            AlertDialogBuilder(context)
                .setTitle(R.string.debug_text_enable_write_external_storage_permission)
                .setPositiveButton(R.string.allow) { _, _ ->
                    if (PermissionChecker.checkWriteExternalStoragePermission(context))
                        return@setPositiveButton
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                .create()
                .show()
        }
    }
}