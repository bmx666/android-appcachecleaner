package com.github.bmx666.appcachecleaner.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsCustomPackageListViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val settingsExtraViewModel: SettingsExtraViewModel = hiltViewModel()
    val showButtonCleanCacheDisabledApps by
        settingsExtraViewModel.showButtonCleanCacheDisabledApps.collectAsState()
    val showButtonStartStopService by
        settingsExtraViewModel.showButtonStartStopService.collectAsState()
    val showButtonCloseApp by
        settingsExtraViewModel.showButtonCloseApp.collectAsState()

    val settingsCustomPackageListViewModel: SettingsCustomPackageListViewModel = hiltViewModel()
    val showButtonCustomList by
        settingsCustomPackageListViewModel.listNames.collectAsState()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constant.Navigation.HELP.name) }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_baseline_help_24),
                            contentDescription = stringResource(R.string.menu_item_help)
                        )
                    }
                    IconButton(onClick = { navController.navigate(Constant.Navigation.SETTINGS.name) }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_baseline_settings_24),
                            contentDescription = stringResource(R.string.menu_item_settings)
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
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 16.dp,
                    )
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "",
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {  },
                ) {
                    Text(text = stringResource(id = R.string.btn_clean_cache_user_apps))
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {  },
                ) {
                    Text(text = stringResource(id = R.string.btn_clean_cache_system_apps))
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {  },
                ) {
                    Text(text = stringResource(id = R.string.btn_clean_cache_all_apps))
                }

                if (showButtonCleanCacheDisabledApps == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {  },
                    ) {
                        Text(text = stringResource(id = R.string.btn_clean_cache_disabled_apps))
                    }

                if (showButtonCustomList?.isNotEmpty() == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {  },
                    ) {
                        Text(text = stringResource(id = R.string.btn_clean_custom_list_cache))
                    }

                Spacer(modifier = Modifier.height(10.dp))

                if (showButtonStartStopService == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {  },
                    ) {
                        Text(text = stringResource(id = R.string.btn_start_accessibility_service))
                    }

                if (showButtonCloseApp == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {  },
                    ) {
                        Text(text = stringResource(id = R.string.btn_close_app))
                    }
            }
        }
    )
}