package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.bmx666.appcachecleaner.R
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun SettingsCustomListEdit(
    title: String,
    summary: String? = null,
    imageVector: ImageVector? = null,
    @StringRes iconDesc: Int? = null,
    dialogTextLabel: String,
    state: StateFlow<Set<String>?>, // current value
    onSave: (String) -> Unit, // method to save the new value
) = SettingsCustomListEditRemove(title, summary, imageVector, iconDesc, dialogTextLabel, state, onSave)

@Composable
internal fun SettingsCustomListRemove(
    title: String,
    summary: String? = null,
    imageVector: ImageVector? = null,
    @StringRes iconDesc: Int? = null,
    dialogTextLabel: String,
    state: StateFlow<Set<String>?>, // current value
    onSave: (String) -> Unit, // method to save the new value
) = SettingsCustomListEditRemove(title, summary, imageVector, iconDesc, dialogTextLabel, state, onSave)

@Composable
private fun SettingsCustomListEditRemove(
    title: String,
    summary: String? = null,
    imageVector: ImageVector? = null,
    @StringRes iconDesc: Int? = null,
    dialogTextLabel: String,
    state: StateFlow<Set<String>?>, // current value
    onSave: (String) -> Unit, // method to save the new value
) {
    val stateValue by state.collectAsState()
    stateValue ?: return

    // if the dialog is visible
    var isDialogShown by remember {
        mutableStateOf(false)
    }

    // if the dialog is visible
    var selectedValue by remember {
        mutableStateOf(stateValue!!.first())
    }

    // conditional visibility in dependence to state
    if (isDialogShown) {
        Dialog(onDismissRequest = {
            // dismiss the dialog on touch outside
            isDialogShown = false
        }) {
            CustomListDropDownDialog(
                label = dialogTextLabel,
                storedValue = stateValue!!,
                onOk = onSave,
                onDismiss = { isDialogShown = false },
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            // clicking on the preference, will show the dialog
            isDialogShown = true
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(alignment = Alignment.CenterVertically)
                    .padding(16.dp),
            ) {
                imageVector?.let {
                    Icon(
                        imageVector = imageVector,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = iconDesc?.let { stringResource(id = it) },
                        modifier = Modifier
                            .size(24.dp)
                            .align(alignment = Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    maxLines = Int.MAX_VALUE,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
    summary?.let {
        Text(
            text = summary,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
internal fun SettingsCustomListAdd(
    title: String,
    summary: String? = null,
    imageVector: ImageVector? = null,
    @StringRes iconDesc: Int? = null,
    dialogTextLabel: String,
    state: StateFlow<Set<String>?>, // current value
    onSave: (String) -> Unit, // method to save the new value
) {
    val stateValue by state.collectAsState()
    stateValue ?: return

    // if the dialog is visible
    var isDialogShown by remember {
        mutableStateOf(false)
    }

    // conditional visibility in dependence to state
    if (isDialogShown) {
        Dialog(onDismissRequest = {
            // dismiss the dialog on touch outside
            isDialogShown = false
        }) {
            TextEditDialog(dialogTextLabel, stateValue!!, onSave) {
                // to dismiss dialog from within
                isDialogShown = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            // clicking on the preference, will show the dialog
            isDialogShown = true
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(alignment = Alignment.CenterVertically)
                    .padding(16.dp),
            ) {
                imageVector?.let {
                    Icon(
                        imageVector = imageVector,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = iconDesc?.let { stringResource(id = it) },
                        modifier = Modifier
                            .size(24.dp)
                            .align(alignment = Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    maxLines = Int.MAX_VALUE,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
    summary?.let {
        Text(
            text = summary,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun TextEditDialog(
    label: String,
    storedValue: Set<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit // internal method to dismiss dialog from within
) {
    // storage for new input
    var currentInput by remember {
        mutableStateOf(TextFieldValue(""))
    }

    // if the input is valid - run the method for current value
    var isValid by remember {
        mutableStateOf(false)
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
                    isValid = it.text.isNotBlank() && !storedValue.contains(it.text)
                    currentInput = it
                },
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

@Composable
internal fun CustomListDropDownDialog(
    label: String,
    storedValue: Set<String>,
    onOk: (String) -> Unit,
    onDismiss: () -> Unit, // internal method to dismiss dialog from within
    onFilter: ((String) -> Unit)? = null,
) {
    // storage for new input
    var selectedValue by remember {
        mutableStateOf(storedValue.first())
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
            ExposedDropdownMenu(
                selectedValue = selectedValue,
                options = storedValue.toList(),
                label = "",
                onValueChangedEvent = { value ->
                    selectedValue = value
                },
            )
            Row(
                modifier = Modifier
                    .padding(top = 16.dp),
            ) {
                Button(
                    onClick = {
                        onDismiss()
                    })
                {
                    Text(
                        text = stringResource(id = android.R.string.cancel),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                onFilter?.let {
                    Button(
                        onClick = {
                            // save and dismiss the dialog
                            onFilter(selectedValue)
                            onDismiss()
                            // disable / enable the button
                        })
                    {
                        Text(
                            text = stringResource(id = R.string.filter),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Button(
                    onClick = {
                        // save and dismiss the dialog
                        onOk(selectedValue)
                        onDismiss()
                        // disable / enable the button
                    })
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