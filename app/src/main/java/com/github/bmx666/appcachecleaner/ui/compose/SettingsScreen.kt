package com.github.bmx666.appcachecleaner.ui.compose

import android.os.Build
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsSwitch
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsCustomPackageListViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraSearchTextViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsFilterViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsScenarioViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsTimeoutViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsUiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val settingsCustomPackageListViewModel: SettingsCustomPackageListViewModel = hiltViewModel()
    val settingsExtraSearchTextViewModel: SettingsExtraSearchTextViewModel = hiltViewModel()
    val settingsExtraViewModel: SettingsExtraViewModel = hiltViewModel()
    val settingsFilterViewModel: SettingsFilterViewModel = hiltViewModel()
    val settingsTimeoutViewModel: SettingsTimeoutViewModel = hiltViewModel()
    val settingsScenarioViewModel: SettingsScenarioViewModel = hiltViewModel()
    val settingsUiViewModel: SettingsUiViewModel = hiltViewModel()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.menu_item_settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                            navController.navigate(Constant.Navigation.HOME.name) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 16.dp
                    )
                    .padding(horizontal = 16.dp)
            ) {
                SettingsGroup(resId = R.string.prefs_ui) {
                    SettingsSwitch(
                        titleResId = R.string.prefs_ui_night_mode,
                        state = settingsUiViewModel.forceNightMode.collectAsState(),
                        onClick = { settingsUiViewModel.toggleForceNightMode() }
                    )
                    HorizontalDivider()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsSwitch(
                            titleResId = R.string.prefs_ui_dynamic_color,
                            state = settingsUiViewModel.dynamicColor.collectAsState(),
                            onClick = { settingsUiViewModel.toggleDynamicColor() }
                        )
                        HorizontalDivider()
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SettingsGroup(resId = R.string.filter) {
                        SettingsSwitch(
                            titleResId = R.string.prefs_title_filter_hide_disabled_apps,
                            // summaryResId = R.string.prefs_summary_filter_min_cache_size,
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
                        SettingsSwitch(
                            titleResId = R.string.prefs_title_filter_show_dialog_to_ignore_app,
                            state = settingsFilterViewModel.showDialogToIgnoreApp.collectAsState(),
                            onClick = { settingsFilterViewModel.toggleShowDialogToIgnoreApp() }
                        )
                    }
                }
            }
        }
    )
}