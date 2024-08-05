package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun EditTextPreference(
    title: String,
    summary: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue(value)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            //style = MaterialTheme.typography.h6,
        )
        summary?.let {
            Text(text = it,
                //style = MaterialTheme.typography.body2,
                //color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                //.border(BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                .padding(8.dp)
        )
    }
}

@Composable
fun EditTextPreferenceDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditTextPreferenceItem(
    title: String,
    summary: String? = null,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column {
        EditTextPreference(
            title = title,
            summary = summary,
            value = value,
            onValueChange = { newValue ->
                showDialog = true
            }
        )

        if (showDialog) {
            EditTextPreferenceDialog(
                title = title,
                initialValue = value,
                onDismiss = { showDialog = false },
                onConfirm = { newValue ->
                    onValueChange(newValue)
                    showDialog = false
                }
            )
        }
    }
}