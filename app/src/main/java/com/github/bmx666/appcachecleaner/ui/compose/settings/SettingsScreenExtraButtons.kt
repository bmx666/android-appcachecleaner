package com.github.bmx666.appcachecleaner.ui.compose.settings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsSwitch
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraViewModel

@Composable
internal fun SettingsScreenExtraButtons() {
    val settingsExtraViewModel: SettingsExtraViewModel = hiltViewModel()

    SettingsGroup(resId = R.string.prefs_show_extra_buttons) {
        SettingsSwitch(
            titleResId = R.string.prefs_title_show_button_clean_cache_disabled_apps,
            state = settingsExtraViewModel.showButtonCleanCacheDisabledApps.collectAsState(),
            onClick = { settingsExtraViewModel.toggleShowButtonCleanCacheDisabledApps() }
        )
        HorizontalDivider()
        SettingsSwitch(
            titleResId = R.string.prefs_title_show_button_start_stop_service,
            summaryResId = R.string.prefs_summary_show_button_start_stop_service,
            state = settingsExtraViewModel.showButtonStartStopService.collectAsState(),
            onClick = { settingsExtraViewModel.toggleShowButtonStartStopService() }
        )
        HorizontalDivider()
        SettingsSwitch(
            titleResId = R.string.prefs_title_show_button_close_app,
            summaryResId = R.string.prefs_summary_show_button_close_app,
            state = settingsExtraViewModel.showButtonCloseApp.collectAsState(),
            onClick = { settingsExtraViewModel.toggleShowButtonCloseApp() }
        )
        HorizontalDivider()
        SettingsSwitch(
            titleResId = R.string.prefs_title_extra_show_button_clear_data,
            summaryResId = R.string.prefs_summary_extra_show_button_clear_data,
            state = settingsExtraViewModel.showButtonClearData.collectAsState(),
            onClick = { settingsExtraViewModel.toggleShowButtonClearData() },
            titleColor = MaterialTheme.colorScheme.error,
            summaryColor = MaterialTheme.colorScheme.error,
            switchColors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.error
            )
        )
    }
}