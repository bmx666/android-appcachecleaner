package com.github.bmx666.appcachecleaner.ui.compose.packagelist

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.view.FilterTextEditDialog
import com.github.bmx666.appcachecleaner.ui.viewmodel.PackageListViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsFilterViewModel
import org.springframework.util.unit.DataSize

@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun PackageListFilter(
    settingsFilterViewModel: SettingsFilterViewModel,
    packageListViewModel: PackageListViewModel,
    isFilterDialogShown: Boolean,
    setFilterDialogShown: (Boolean) -> Unit,
) {
    // conditional visibility in dependence to state
    if (isFilterDialogShown) {
        val minCacheSizeString by settingsFilterViewModel.minCacheSizeString.collectAsState()

        Dialog(onDismissRequest = {
            // dismiss the dialog on touch outside
            setFilterDialogShown(false)
        }) {
            FilterTextEditDialog(
                label = stringResource(
                    id = R.string.prefs_error_convert_filter_min_cache_size
                ),
                placeholder = {
                    Text(text = minCacheSizeString ?: "0 KB")
                },
                storedValue = packageListViewModel.filterByCacheSizeString.collectAsState(),
                onSave = { str ->
                    val dataSize = DataSize.parse(str)
                    val minCacheSizeBytes = dataSize.toBytes()
                    packageListViewModel.filterByCacheSize(minCacheSizeBytes)
                },
                onCheck = { str ->
                    try {
                        DataSize.parse(str)
                        true
                    } catch (e: Exception) {
                        false
                    }
                },
            ) {
                // to dismiss dialog from within
                setFilterDialogShown(false)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun PackageListFilterIcon(
    setFilterDialogShown: (Boolean) -> Unit,
) {
    IconButton(
        onClick = {
            setFilterDialogShown(true)
        })
    {
        Icon(
            imageVector = Icons.Default.FilterAlt,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            contentDescription = stringResource(R.string.filter)
        )
    }
}