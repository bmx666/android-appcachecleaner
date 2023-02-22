package com.github.bmx666.appcachecleaner.ui.dialog

import android.content.Context
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager

class FirstBootDialogBuilder {
    companion object {
        @JvmStatic
        fun buildHelpCustomizedSettingsUIDialog(context: Context) {
            AlertDialogBuilder(context)
                .setTitle(R.string.dialog_help_customized_settings_ui_title)
                .setMessage(R.string.dialog_help_customized_settings_ui_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    SharedPreferencesManager.FirstBoot.hideDialogHelpCustomizedSettingsUI(context)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
                .show()
        }
    }
}