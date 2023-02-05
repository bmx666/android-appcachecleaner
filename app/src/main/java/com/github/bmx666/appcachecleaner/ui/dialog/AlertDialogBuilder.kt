package com.github.bmx666.appcachecleaner.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import com.github.bmx666.appcachecleaner.R

class AlertDialogBuilder(context: Context):
    AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialog)) {

    companion object {

        // Touch target size
        // https://support.google.com/accessibility/android/answer/7101858
        @JvmStatic
        internal fun getMinTouchTargetSize(context: Context): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                context.resources.displayMetrics
            ).toInt()
        }
    }
}