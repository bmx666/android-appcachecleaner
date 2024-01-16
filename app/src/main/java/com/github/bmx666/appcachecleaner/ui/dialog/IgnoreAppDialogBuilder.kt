package com.github.bmx666.appcachecleaner.ui.dialog

import android.app.Dialog
import android.content.Context
import androidx.core.text.HtmlCompat
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager

class IgnoreAppDialogBuilder {

    companion object {

        @JvmStatic
        fun buildIgnoreAppDialog(context: Context,
                                 pkgName: String): Dialog {
            val message = HtmlCompat.fromHtml(
                context.getString(R.string.dialog_ignore_app_messages, pkgName),
                HtmlCompat.FROM_HTML_MODE_COMPACT)

            return AlertDialogBuilder(context)
                .setTitle(R.string.dialog_ignore_app_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val list = SharedPreferencesManager.Filter.getListOfIgnoredApps(context)
                            as MutableSet<String>
                    list.add(pkgName)
                    SharedPreferencesManager.Filter.setListOfIgnoredApps(context, list)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
        }
    }
}