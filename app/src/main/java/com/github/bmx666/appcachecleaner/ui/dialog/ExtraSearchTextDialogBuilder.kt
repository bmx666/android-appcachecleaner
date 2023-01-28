package com.github.bmx666.appcachecleaner.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.widget.EditText
import androidx.appcompat.view.ContextThemeWrapper
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import java.util.*

class ExtraSearchTextDialogBuilder {
    companion object {

        // Touch target size
        // https://support.google.com/accessibility/android/answer/7101858
        @JvmStatic
        private fun getMinTouchTargetSize(context: Context): Int {
            return TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        48f,
                        context.resources.displayMetrics
                    ).toInt()
        }

        @JvmStatic
        fun buildStorageDialog(context: Context, locale: Locale) {
            val inputEditText = EditText(context).apply {
                val text = SharedPreferencesManager.ExtraSearchText.getStorage(context, locale)
                if (text?.isNotEmpty() == true)
                    setText(text)
                else {
                    val resId = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                            R.string.storage_settings_for_app
                        else -> R.string.storage_label
                    }
                    hint = context.getText(resId)
                }

                minHeight = getMinTouchTargetSize(context)
                minWidth = getMinTouchTargetSize(context)
            }

            AlertDialogBuilder(context)
                .setTitle(context.getText(R.string.dialog_extra_search_text_title))
                .setMessage(context.getString(
                    R.string.dialog_extra_search_text_message,
                    locale.displayLanguage, locale.displayCountry,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        context.getText(R.string.storage_settings_for_app)
                    else
                        context.getText(R.string.storage_label)))
                .setView(inputEditText)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.saveStorage(
                        context, locale, inputEditText.text
                    )
                }
                .setNegativeButton(R.string.dialog_extra_search_text_btn_remove) { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.removeStorage(
                        context, locale
                    )
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildClearCacheDialog(context: Context, locale: Locale) {
            val inputEditText = EditText(context).apply {
                val text = SharedPreferencesManager.ExtraSearchText.getClearCache(context, locale)
                if (text?.isNotEmpty() == true)
                    setText(text)
                else
                    hint = context.getText(R.string.clear_cache_btn_text)

                minHeight = getMinTouchTargetSize(context)
                minWidth = getMinTouchTargetSize(context)
            }

            AlertDialogBuilder(context)
                .setTitle(context.getText(R.string.dialog_extra_search_text_title))
                .setMessage(context.getString(
                    R.string.dialog_extra_search_text_message,
                    locale.displayLanguage, locale.displayCountry,
                    context.getText(R.string.clear_cache_btn_text)))
                .setView(inputEditText)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.saveClearCache(
                        context, locale, inputEditText.text
                    )
                }
                .setNegativeButton(R.string.dialog_extra_search_text_btn_remove) { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.removeClearCache(
                        context, locale
                    )
                }
                .create()
                .show()
        }
    }
}