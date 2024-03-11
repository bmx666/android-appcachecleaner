package com.github.bmx666.appcachecleaner.ui.dialog

import android.app.Dialog
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import com.github.bmx666.appcachecleaner.R

class CustomListDialogBuilder {

    companion object {

        @JvmStatic
        @UiContext
        @UiThread
        private fun getCustomListSpinner(context: Context, names: List<String>): Spinner {
            return Spinner(context).apply {
                adapter = ArrayAdapter(context,
                    android.R.layout.simple_spinner_dropdown_item,
                    names.toTypedArray())
            }
        }

        @JvmStatic
        @UiContext
        @UiThread
        fun buildAddDialog(context: Context, onOkClick: (String?) -> Unit): Dialog {
            val inputEditText = EditText(context).apply {
                hint = context.getString(android.R.string.unknownName)
                minHeight = AlertDialogBuilder.getMinTouchTargetSize(context)
                minWidth = AlertDialogBuilder.getMinTouchTargetSize(context)
            }

            return AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_add)
                .setMessage(R.string.dialog_message_custom_list_add)
                .setView(inputEditText)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(inputEditText.text?.trim().toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
        }

        @JvmStatic
        @UiContext
        @UiThread
        fun buildEditDialog(context: Context,
                            names: List<String>,
                            onOkClick: (String?) -> Unit): Dialog {
            val customListSpinner = getCustomListSpinner(context, names)

            return AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_edit)
                .setMessage(R.string.dialog_message_custom_list_edit)
                .setView(customListSpinner)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(customListSpinner.selectedItem.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
        }

        @JvmStatic
        @UiContext
        @UiThread
        fun buildRemoveDialog(context: Context,
                              names: List<String>,
                              onOkClick: (String?) -> Unit): Dialog {
            val customListSpinner = getCustomListSpinner(context, names)

            return AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_remove)
                .setMessage(R.string.dialog_message_custom_list_remove)
                .setView(customListSpinner)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(customListSpinner.selectedItem.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
        }

        @JvmStatic
        @UiContext
        @UiThread
        fun buildCleanCacheDialog(context: Context,
                                  names: List<String>,
                                  onOkClick: (String?) -> Unit): Dialog {
            val customListSpinner = getCustomListSpinner(context, names)

            return AlertDialogBuilder(context)
                .setTitle(R.string.dialog_title_custom_list_clean_cache)
                .setMessage(R.string.dialog_message_custom_list_clean_cache)
                .setView(customListSpinner)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(customListSpinner.selectedItem.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
        }
    }
}