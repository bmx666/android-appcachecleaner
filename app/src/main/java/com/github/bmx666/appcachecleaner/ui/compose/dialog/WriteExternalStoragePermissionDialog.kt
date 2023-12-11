package com.github.bmx666.appcachecleaner.ui.compose.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.view.StyledText
import com.github.bmx666.appcachecleaner.util.PermissionChecker
import com.github.bmx666.appcachecleaner.util.charSequenceResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun WriteExternalStoragePermissionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    if (showDialog) {
        Dialog(onDismissRequest = {
            onDismiss()
        }) {
            Surface(
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    StyledText(text = charSequenceResource(
                        R.string.debug_text_enable_write_external_storage_permission)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                onDismiss()
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (PermissionChecker.checkWriteExternalStoragePermission(context))
                                        return@launch
                                    CoroutineScope(Dispatchers.Main).launch {
                                        //requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.allow),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}