package com.github.bmx666.appcachecleaner.ui.compose.settings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsSwitch
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraViewModel

@Composable
internal fun SettingsScreenExtraActions() {
    val settingsExtraViewModel: SettingsExtraViewModel = hiltViewModel()

    SettingsGroup(resId = R.string.prefs_extra_actions) {
        SettingsSwitch(
            titleResId = R.string.prefs_title_extra_action_force_stop_apps,
            summaryResId = R.string.prefs_summary_extra_action_force_stop_apps,
            state = settingsExtraViewModel.actionForceStopApps.collectAsState(),
            onClick = { settingsExtraViewModel.toggleActionForceStopApps() }
        )
        SettingsSwitch(
            titleResId = R.string.prefs_title_extra_action_stop_service,
            summaryResId = R.string.prefs_summary_extra_action_stop_service,
            state = settingsExtraViewModel.actionStopService.collectAsState(),
            onClick = { settingsExtraViewModel.toggleActionStopService() }
        )
        HorizontalDivider()
        SettingsSwitch(
            titleResId = R.string.prefs_title_extra_action_close_app,
            summaryResId = R.string.prefs_summary_extra_action_close_app,
            state = settingsExtraViewModel.actionCloseApp.collectAsState(),
            onClick = { settingsExtraViewModel.toggleActionCloseApp() }
        )
    }
}