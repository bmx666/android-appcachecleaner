package com.github.bmx666.appcachecleaner.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

@Composable
@ReadOnlyComposable
internal fun charSequenceResource(@StringRes id: Int): CharSequence {
    val context = LocalContext.current
    val resources = context.resources
    return resources.getText(id)
}