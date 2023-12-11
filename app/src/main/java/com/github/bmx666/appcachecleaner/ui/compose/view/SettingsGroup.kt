package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun SettingsGroup(
    @StringRes resId: Int,
    // to accept only composables compatible with column
    content: @Composable ColumnScope.() -> Unit
){
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(id = resId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        supportingContent = {
            Column {
                content()
            }
        },
        leadingContent = {},
    )
}