package com.github.bmx666.appcachecleaner.ui.dialog

import android.app.AlertDialog
import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.github.bmx666.appcachecleaner.R

class AlertDialogBuilder(context: Context):
    AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialog))