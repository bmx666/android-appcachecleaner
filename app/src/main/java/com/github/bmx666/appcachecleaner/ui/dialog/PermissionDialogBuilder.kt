package com.github.bmx666.appcachecleaner.ui.dialog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.util.PermissionChecker

class PermissionDialogBuilder {
    companion object {
        @JvmStatic
        fun buildAccessibilityPermissionDialog(context: Context) {
            AlertDialogBuilder(context)
                .setTitle(R.string.text_enable_accessibility_title)
                .setMessage(R.string.text_enable_accessibility_message)
                .setPositiveButton(R.string.allow) { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    context.startActivity(intent)
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
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    context.startActivity(intent)
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