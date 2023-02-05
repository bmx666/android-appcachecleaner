package com.github.bmx666.appcachecleaner.ui.dialog

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager

class CustomListDialogBuilder {

    companion object {

        @JvmStatic
        private fun getCustomListSpinner(context: Context): Spinner {
            val list = SharedPreferencesManager.PackageList.getNames(context).toMutableList()
            list.sort()
            return Spinner(context).apply {
                adapter = ArrayAdapter(context,
                    android.R.layout.simple_spinner_dropdown_item,
                    list.toTypedArray())
            }
        }

        @JvmStatic
        fun buildAddDialog(context: Context, onOkClick: (String?) -> Unit) {
            val inputEditText = EditText(context).apply {
                hint = context.getString(android.R.string.unknownName)
                minHeight = AlertDialogBuilder.getMinTouchTargetSize(context)
                minWidth = AlertDialogBuilder.getMinTouchTargetSize(context)
            }

            AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_add)
                .setMessage(R.string.dialog_message_custom_list_add)
                .setView(inputEditText)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(inputEditText.text?.trim().toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildEditDialog(context: Context, onOkClick: (String?) -> Unit) {
            val customListSpinner = getCustomListSpinner(context)

            AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_edit)
                .setMessage(R.string.dialog_message_custom_list_edit)
                .setView(customListSpinner)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(customListSpinner.selectedItem.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildRemoveDialog(context: Context, onOkClick: (String?) -> Unit) {
            val customListSpinner = getCustomListSpinner(context)

            AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_remove)
                .setMessage(R.string.dialog_message_custom_list_remove)
                .setView(customListSpinner)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(customListSpinner.selectedItem.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
                .show()
        }

        @JvmStatic
        fun buildCleanCacheDialog(context: Context, onOkClick: (String?) -> Unit) {
            val customListSpinner = getCustomListSpinner(context)

            AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_clean_cache)
                .setMessage(R.string.dialog_message_custom_list_clean_cache)
                .setView(customListSpinner)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(customListSpinner.selectedItem.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
                .show()
        }
    }
}