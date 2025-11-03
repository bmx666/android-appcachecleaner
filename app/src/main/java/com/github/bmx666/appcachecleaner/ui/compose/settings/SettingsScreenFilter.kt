package com.github.bmx666.appcachecleaner.ui.compose.settings

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsEditText
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsSwitch
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsText
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsFilterViewModel
import org.springframework.util.unit.DataSize

@Composable
internal fun SettingsScreenFilter(navController: NavController) {
    val settingsFilterViewModel: SettingsFilterViewModel = hiltViewModel()

    SettingsGroup(resId = R.string.filter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val minCacheSizeString by settingsFilterViewModel.minCacheSizeString.collectAsState()

            SettingsEditText(
                name = stringResource(id = R.string.prefs_title_filter_min_cache_size),
                summary = minCacheSizeString?.let { value ->
                    stringResource(id = R.string.prefs_summary_filter_min_cache_size, value)
                },
                dialogTextLabel = stringResource(
                    id = R.string.prefs_error_convert_filter_min_cache_size),
                dialogTextPlaceholder = {
                    Text(text = "0 KB")
                },
                state = settingsFilterViewModel.minCacheSizeString.collectAsState(),
                onSave = { str ->
                    val dataSize = DataSize.parse(str)
                    val minCacheSize = dataSize.toBytes()
                    if (minCacheSize > 0L)
                        settingsFilterViewModel.setMinCacheSizeBytes(
                            if (minCacheSize > 1024L) minCacheSize else 1024L)
                    else
                        settingsFilterViewModel.removeMinCacheSizeBytes()
                },
                onCheck = { str ->
                    try {
                        DataSize.parse(str)
                        true
                    } catch (e: Exception) {
                        false
                    }
                },
            )
            HorizontalDivider()
        }
        SettingsSwitch(
            titleResId = R.string.prefs_title_filter_hide_disabled_apps,
            state = settingsFilterViewModel.hideDisabledApps.collectAsState(),
            onClick = { settingsFilterViewModel.toggleHideDisabledApps() }
        )
        HorizontalDivider()
        SettingsSwitch(
            titleResId = R.string.prefs_title_filter_hide_ignored_apps,
            state = settingsFilterViewModel.hideIgnoredApps.collectAsState(),
            onClick = { settingsFilterViewModel.toggleHideIgnoredApps() }
        )
        HorizontalDivider()
        SettingsText(
            title = stringResource(id = R.string.prefs_title_filter_list_of_ignored_apps_edit),
            summary = stringResource(id = R.string.prefs_summary_filter_list_of_ignored_apps_edit),
            imageVector = Icons.Default.Edit,
            onClick = {
                val root = Constant.Navigation.PACKAGE_LIST
                val action = Constant.PackageListAction.IGNORED_APPS
                navController.navigate(
                    route = "$root/$action"
                )
            },
        )
    }
}