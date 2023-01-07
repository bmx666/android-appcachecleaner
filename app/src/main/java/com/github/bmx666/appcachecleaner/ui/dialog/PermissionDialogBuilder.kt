package com.github.bmx666.appcachecleaner.ui.dialog

import android.Manifest
import android.app.AlertDialog
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
            AlertDialog.Builder(context)
                .setTitle(context.getText(R.string.text_enable_accessibility_permission))
                .setMessage(context.getString(R.string.text_enable_accessibility)
                    .plus(System.getProperty("line.separator"))
                    .plus(context.getString(R.string.text_enable_accessibility_explanation)))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    context.startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildUsageStatsPermissionDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle(context.getText(R.string.text_enable_usage_stats_permission))
                .setMessage(context.getString(R.string.text_enable_usage_stats)
                    .plus(System.getProperty("line.separator"))
                    .plus(context.getString(R.string.text_enable_usage_stats_explanation)))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    context.startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildWriteExternalStoragePermissionDialog(context: Context,
            requestPermissionLauncher: ActivityResultLauncher<String>) {
            AlertDialog.Builder(context)
                .setTitle(context.getText(R.string.debug_text_enable_write_external_storage_permission))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (PermissionChecker.checkWriteExternalStoragePermission(context))
                        return@setPositiveButton
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                .create()
                .show()
        }
    }
}