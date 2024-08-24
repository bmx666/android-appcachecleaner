package com.github.bmx666.appcachecleaner.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.settings.SettingsScreenCustomPackageList
import com.github.bmx666.appcachecleaner.ui.compose.settings.SettingsScreenExtraActions
import com.github.bmx666.appcachecleaner.ui.compose.settings.SettingsScreenExtraButtons
import com.github.bmx666.appcachecleaner.ui.compose.settings.SettingsScreenExtraSearchText
import com.github.bmx666.appcachecleaner.ui.compose.settings.SettingsScreenFilter
import com.github.bmx666.appcachecleaner.ui.compose.settings.SettingsScreenOther
import com.github.bmx666.appcachecleaner.ui.compose.settings.SettingsScreenUi
import com.github.bmx666.appcachecleaner.ui.compose.view.CenterAlignedTopAppBar
import com.github.bmx666.appcachecleaner.ui.compose.view.GoBackIconButton
import com.github.bmx666.appcachecleaner.ui.viewmodel.LocaleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    localeViewModel: LocaleViewModel,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.menu_item_settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    GoBackIconButton(navController)
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
                SettingsScreenUi()
                HorizontalDivider()
                SettingsScreenFilter(navController)
                HorizontalDivider()
                SettingsScreenExtraSearchText(localeViewModel)
                HorizontalDivider()
                SettingsScreenExtraButtons()
                HorizontalDivider()
                SettingsScreenExtraActions()
                HorizontalDivider()
                SettingsScreenCustomPackageList(navController)
                HorizontalDivider()
                SettingsScreenOther()
            }
        }
    )
}