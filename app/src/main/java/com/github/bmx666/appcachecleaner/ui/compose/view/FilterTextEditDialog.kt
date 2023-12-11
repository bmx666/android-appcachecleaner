package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
internal fun FilterTextEditDialog(
    label: String,
    placeholder: @Composable (() -> Unit)?,
    storedValue: State<String?>,
    onSave: (String) -> Unit,
    onCheck: (String) -> Boolean,
    onDismiss: () -> Unit // internal method to dismiss dialog from within
) {
    // storage for new input
    var currentInput by remember {
        mutableStateOf(TextFieldValue(storedValue.value ?: ""))
    }

    // if the input is valid - run the method for current value
    var isValid by remember {
        mutableStateOf(onCheck(storedValue.value ?: ""))
    }

    Surface(
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = currentInput,
                onValueChange = {
                    // check on change, if the value is valid
                    isValid = onCheck(it.text)
                    currentInput = it
                },
                placeholder = placeholder,
            )
            Row(
                modifier = Modifier
                    .padding(top = 16.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        // save and dismiss the dialog
                        onSave(currentInput.text)
                        onDismiss()
                        // disable / enable the button
                    },
                    enabled = isValid)
                {
                    Text(
                        text = stringResource(id = android.R.string.ok),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}