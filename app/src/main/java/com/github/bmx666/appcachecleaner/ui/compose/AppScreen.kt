package com.github.bmx666.appcachecleaner.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.theme.AppTheme
import com.github.bmx666.appcachecleaner.ui.viewmodel.CleanResultViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.FirstBootViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.LocaleViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsUiViewModel
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerActivityHelper
import com.github.bmx666.appcachecleaner.util.decode

@Composable
fun AppScreen(
    localBroadcastManager: LocalBroadcastManagerActivityHelper,
    localeViewModel: LocaleViewModel,
    cleanResultViewModel: CleanResultViewModel,
) {
    val settingsUiViewModel: SettingsUiViewModel = hiltViewModel()
    val firstBootViewModel: FirstBootViewModel = hiltViewModel()

    val forceNightMode by settingsUiViewModel.forceNightMode.collectAsState()
    val dynamicColor by settingsUiViewModel.dynamicColor.collectAsState()
    val contrast by settingsUiViewModel.contrast.collectAsState()
    val isFirstBoot by firstBootViewModel.firstBoot.collectAsState()

    AppTheme(forceNightMode, dynamicColor, contrast) {
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
                    localBroadcastManager = localBroadcastManager,
                    cleanResultViewModel = cleanResultViewModel,
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
                    localeViewModel = localeViewModel,
                )
            }
            composable(
                route = "${Constant.Navigation.PACKAGE_LIST}/{action}?name={name}",
                arguments = listOf(
                    navArgument("action") {
                        type = NavType.StringType
                    },
                    navArgument("name") {
                        type = NavType.StringType
                        nullable = true
                    },
                ),
            ) { backStackEntry ->
                val action = backStackEntry.arguments?.getString("action")
                val name = backStackEntry.arguments?.getString("name")?.decode()
                PackageListScreen(
                    navController = navController,
                    localBroadcastManager = localBroadcastManager,
                    action = action,
                    name = name,
                )
            }
        }
    }
}