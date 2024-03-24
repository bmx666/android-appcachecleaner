package com.github.bmx666.appcachecleaner.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.theme.AppTheme
import com.github.bmx666.appcachecleaner.ui.viewmodel.FirstBootViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsUiViewModel

@Composable
fun AppScreen() {
    val settingsUiViewModel: SettingsUiViewModel = hiltViewModel()
    val firstBootViewModel: FirstBootViewModel = hiltViewModel()

    val forceNightMode by settingsUiViewModel.forceNightMode.collectAsState()
    val dynamicColor by settingsUiViewModel.dynamicColor.collectAsState()
    val isFirstBoot by firstBootViewModel.firstBoot.collectAsState()

    AppTheme(forceNightMode, dynamicColor) {
        val navController = rememberNavController()

        val startDestination = when (isFirstBoot) {
            true -> Constant.Navigation.FIRST_BOOT.name
            else -> Constant.Navigation.HOME.name
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Constant.Navigation.FIRST_BOOT.name) {
                FirstBootScreen(
                    navController = navController,
                )
            }
            composable(Constant.Navigation.HOME.name) {
                HomeScreen(
                    navController = navController,
                )
            }
            composable(Constant.Navigation.HELP.name) {
                HelpScreen(
                    navController = navController,
                )
            }
            composable(Constant.Navigation.SETTINGS.name) {
                SettingsScreen(
                    navController = navController,
                )
            }
            //composable("package_list") { PackageListScreen(navController) }
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            //    showFilterDialog()
        }
    }
}