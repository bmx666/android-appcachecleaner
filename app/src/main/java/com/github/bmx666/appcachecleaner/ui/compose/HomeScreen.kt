package com.github.bmx666.appcachecleaner.ui.compose

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.compose.dialog.AccessibilityPermissionDialog
import com.github.bmx666.appcachecleaner.ui.compose.dialog.InterruptedBySystemDialog
import com.github.bmx666.appcachecleaner.ui.compose.dialog.UsageStatsPermissionDialog
import com.github.bmx666.appcachecleaner.ui.compose.view.CustomListDropDownDialog
import com.github.bmx666.appcachecleaner.ui.compose.view.TopAppBar
import com.github.bmx666.appcachecleaner.ui.compose.view.goBack
import com.github.bmx666.appcachecleaner.ui.viewmodel.CleanResultViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.PermissionViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsCustomPackageListViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraViewModel
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerActivityHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    localBroadcastManager: LocalBroadcastManagerActivityHelper,
    cleanResultViewModel: CleanResultViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val settingsExtraViewModel: SettingsExtraViewModel = hiltViewModel()
    val showButtonCleanCacheDisabledApps by
        settingsExtraViewModel.showButtonCleanCacheDisabledApps.collectAsState()
    val showButtonStartStopService by
        settingsExtraViewModel.showButtonStartStopService.collectAsState()
    val showButtonCloseApp by
        settingsExtraViewModel.showButtonCloseApp.collectAsState()

    val settingsCustomPackageListViewModel: SettingsCustomPackageListViewModel = hiltViewModel()
    val customListNames by settingsCustomPackageListViewModel.listNames.collectAsState()
    var isCustomListDialogShown by remember { mutableStateOf(false) }
    var selectedCustomListName by remember { mutableStateOf("") }
    val selectedCustomList by settingsCustomPackageListViewModel
        .getCustomPackageList(selectedCustomListName).collectAsState()
    var selectedCustomListNameForFilter by remember { mutableStateOf(false) }

    val permissionViewModel: PermissionViewModel = hiltViewModel()
    val permissionCheckerIsReady by permissionViewModel.isReady.collectAsState()
    val hasAccessibilityPermission by permissionViewModel.hasAccessibilityPermission.collectAsState()
    val hasUsageStatsPermission by permissionViewModel.hasUsageStatsPermission.collectAsState()

    var isAccessibilityPermissionDialogShown by remember { mutableStateOf(false) }
    var isUsageStatsPermissionDialogShown by remember { mutableStateOf(false) }
    val onCheckPermission: () -> Boolean = {
        when {
            hasUsageStatsPermission != true -> {
                isUsageStatsPermissionDialogShown = true
                false
            }
            hasAccessibilityPermission != true -> {
                isAccessibilityPermissionDialogShown = true
                false
            }
            else -> true
        }
    }

    val showInterruptedBySystemDialog by cleanResultViewModel.isInterruptedBySystem.collectAsState()

    // DisposableEffect to observe lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check permissions when the screen is resumed
                permissionViewModel.checkAccessibilityPermission()
                permissionViewModel.checkUsageStatsPermission()
            }
        }

        // Add observer
        lifecycleOwner.lifecycle.addObserver(observer)

        // Remove observer when the composable is disposed
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Use LaunchedEffect to wait for selectedCustomList to be updated
    LaunchedEffect(selectedCustomListName) {
        snapshotFlow { selectedCustomList }
            .filterNotNull() // Only proceed when selectedCustomList is non-null
            .distinctUntilChanged() // Only react to actual changes in the list
            .collectLatest { list ->
                if (list.isNotEmpty()) {
                    if (selectedCustomListNameForFilter) {
                        val root = Constant.Navigation.PACKAGE_LIST
                        val action = Constant.PackageListAction.CUSTOM_LIST_FILTER
                        navController.navigate(
                            route = "$root/$action?name=$selectedCustomListName"
                        )
                    } else {
                        val pkgList = list.toMutableList()
                        localBroadcastManager.sendPackageListToClearCache(pkgList as ArrayList<String>)
                    }
                    // reset string to avoid misbehavior
                    selectedCustomListName = ""
                }
            }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Constant.Navigation.HELP.name) })
                    {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentDescription = stringResource(R.string.menu_item_help)
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(Constant.Navigation.SETTINGS.name) })
                    {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentDescription = stringResource(R.string.menu_item_settings)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            AccessibilityPermissionDialog(
                showDialog = isAccessibilityPermissionDialogShown) {
                isAccessibilityPermissionDialogShown = false
            }
            UsageStatsPermissionDialog(
                showDialog = isUsageStatsPermissionDialogShown) {
                isUsageStatsPermissionDialogShown = false
            }
            InterruptedBySystemDialog(
                showDialog = showInterruptedBySystemDialog) {
                cleanResultViewModel.resetInterruptedBySystemDialog()
            }

            if (isCustomListDialogShown) {
                Dialog(onDismissRequest = {
                    // dismiss the dialog on touch outside
                    isCustomListDialogShown = false
                }) {
                    CustomListDropDownDialog(
                        label = stringResource(
                            id = R.string.dialog_message_custom_list_clean_cache),
                        storedValue = customListNames ?: emptySet(),
                        onOk = { name ->
                            selectedCustomListName = name
                            selectedCustomListNameForFilter = false
                        },
                        onDismiss = { isCustomListDialogShown = false },
                        onFilter = { name ->
                            selectedCustomListName = name
                            selectedCustomListNameForFilter = true
                        },
                    )
                }
            }

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
                    text = cleanResultViewModel.titleText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        cleanResultViewModel.resetTitle()
                        if (onCheckPermission()) {
                            val root = Constant.Navigation.PACKAGE_LIST
                            val action = Constant.PackageListAction.USER_APPS
                            navController.navigate(
                                route = "$root/$action"
                            )
                        }
                    },
                    enabled = permissionCheckerIsReady,
                ) {
                    Text(text = stringResource(id = R.string.btn_clean_cache_user_apps))
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        cleanResultViewModel.resetTitle()
                        if (onCheckPermission()) {
                            val root = Constant.Navigation.PACKAGE_LIST
                            val action = Constant.PackageListAction.SYSTEM_APPS
                            navController.navigate(
                                route = "$root/$action"
                            )
                        }
                    },
                    enabled = permissionCheckerIsReady,
                ) {
                    Text(text = stringResource(id = R.string.btn_clean_cache_system_apps))
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        cleanResultViewModel.resetTitle()
                        if (onCheckPermission()) {
                            val root = Constant.Navigation.PACKAGE_LIST
                            val action = Constant.PackageListAction.ALL_APPS
                            navController.navigate(
                                route = "$root/$action"
                            )
                        }
                    },
                    enabled = permissionCheckerIsReady,
                ) {
                    Text(text = stringResource(id = R.string.btn_clean_cache_all_apps))
                }

                if (showButtonCleanCacheDisabledApps == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            cleanResultViewModel.resetTitle()
                            if (onCheckPermission()) {
                                val root = Constant.Navigation.PACKAGE_LIST
                                val action = Constant.PackageListAction.DISABLED_APPS
                                navController.navigate(
                                    route = "$root/$action"
                                )
                            }
                        },
                        enabled = permissionCheckerIsReady,
                    ) {
                        Text(text = stringResource(id = R.string.btn_clean_cache_disabled_apps))
                    }

                if (customListNames?.isNotEmpty() == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            cleanResultViewModel.resetTitle()
                            if (onCheckPermission()) {
                                isCustomListDialogShown = true
                            }
                        },
                        enabled = permissionCheckerIsReady,
                    ) {
                        Text(text = stringResource(id = R.string.btn_clean_custom_list_cache))
                    }

                Spacer(modifier = Modifier.height(10.dp))

                if (showButtonStartStopService == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            cleanResultViewModel.resetTitle()
                            when (hasAccessibilityPermission) {
                                true -> {
                                    localBroadcastManager.disableAccessibilityService()
                                    goBack(navController)
                                }
                                false -> {
                                    isAccessibilityPermissionDialogShown = true
                                }
                                else -> {}
                            }
                        },
                        enabled = permissionCheckerIsReady,
                    ) {
                        Text(text = when (hasAccessibilityPermission) {
                                true -> stringResource(id = R.string.btn_stop_accessibility_service)
                                else -> stringResource(id = R.string.btn_start_accessibility_service)
                            }
                        )
                    }

                if (showButtonCloseApp == true)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            (context as? Activity)?.finishAffinity()
                        },
                    ) {
                        Text(text = stringResource(id = R.string.btn_close_app))
                    }
            }
        }
    )
}