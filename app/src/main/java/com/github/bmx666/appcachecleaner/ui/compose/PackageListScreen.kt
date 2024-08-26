package com.github.bmx666.appcachecleaner.ui.compose

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.compose.packagelist.PackageListFilter
import com.github.bmx666.appcachecleaner.ui.compose.packagelist.PackageListFilterIcon
import com.github.bmx666.appcachecleaner.ui.compose.packagelist.PackageListPackageList
import com.github.bmx666.appcachecleaner.ui.compose.packagelist.PackageListSearchIcon
import com.github.bmx666.appcachecleaner.ui.compose.view.GoBackIconButton
import com.github.bmx666.appcachecleaner.ui.compose.view.TopAppBar
import com.github.bmx666.appcachecleaner.ui.compose.view.goBack
import com.github.bmx666.appcachecleaner.ui.viewmodel.PackageListViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsFilterViewModel
import com.github.bmx666.appcachecleaner.util.ActivityHelper
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerActivityHelper
import com.github.bmx666.appcachecleaner.util.getEnumValueOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PackageListScreen(
    navController: NavHostController,
    localBroadcastManager: LocalBroadcastManagerActivityHelper,
    action: String?,
    name: String?,
) {
    val context = LocalContext.current
    val packageListViewModel: PackageListViewModel = hiltViewModel()
    val settingsFilterViewModel: SettingsFilterViewModel = hiltViewModel()

    val packageListAction = action?.getEnumValueOrNull<Constant.PackageListAction>()

    // hide cache size for the list of ignored apps
    val showCacheSize = when(packageListAction) {
        Constant.PackageListAction.CUSTOM_LIST_ADD,
        Constant.PackageListAction.CUSTOM_LIST_EDIT,
        Constant.PackageListAction.IGNORED_APPS ->
            false
        else -> true
    }

    LaunchedEffect(action) {
        packageListAction?.let { value ->
            when (value) {
                Constant.PackageListAction.USER_APPS ->
                    packageListViewModel.loadUserApps()
                Constant.PackageListAction.SYSTEM_APPS ->
                    packageListViewModel.loadSystemApps()
                Constant.PackageListAction.ALL_APPS ->
                    packageListViewModel.loadAllApps()
                Constant.PackageListAction.DISABLED_APPS ->
                    packageListViewModel.loadDisabledApps()
                Constant.PackageListAction.IGNORED_APPS ->
                    packageListViewModel.loadIgnoredApps()
                Constant.PackageListAction.CUSTOM_LIST_ADD -> {
                    packageListViewModel.loadCustomListAppsAdd()
                }
                Constant.PackageListAction.CUSTOM_LIST_EDIT -> {
                    name?.let {
                        packageListViewModel.loadCustomListAppsEdit(name)
                    } ?: goBack(navController)
                }
            }
        } ?: goBack(navController)
    }

    val isLoading by packageListViewModel.isLoading.collectAsState()
    val isProcessing by packageListViewModel.isProcessing.collectAsState()
    val pkgListCurrentVisible by packageListViewModel.pkgListCurrentVisible.collectAsState()
    val pkgListChecked by packageListViewModel.pkgListChecked.collectAsState()

    val checkedTotalCacheSizeString by packageListViewModel.checkedTotalCacheSizeString.collectAsState()
    val fixedTitle = stringResource(R.string.app_name)
    val title = when (packageListAction) {
        Constant.PackageListAction.USER_APPS,
        Constant.PackageListAction.SYSTEM_APPS,
        Constant.PackageListAction.ALL_APPS,
        Constant.PackageListAction.DISABLED_APPS -> {
            checkedTotalCacheSizeString?.let {
                String.format("%s (%s)", fixedTitle, checkedTotalCacheSizeString)
            } ?: fixedTitle
        }
        Constant.PackageListAction.IGNORED_APPS ->
            stringResource(id = R.string.text_list_ignored_apps)
        Constant.PackageListAction.CUSTOM_LIST_ADD,
        Constant.PackageListAction.CUSTOM_LIST_EDIT -> {
            name ?: fixedTitle
        }
        null -> fixedTitle
    }

    val showToastCustomListEmpty: () -> Unit = {
        Toast.makeText(context,
            context.getString(R.string.toast_custom_list_add_list_empty),
            Toast.LENGTH_SHORT).show()
    }
    val showToastCustomListSave: (String) -> Unit = { savedListName ->
        Toast.makeText(context,
            context.getString(R.string.toast_custom_list_has_been_saved, savedListName),
            Toast.LENGTH_SHORT).show()
    }

    // if the dialog is visible
    var isFilterDialogShown by remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        PackageListFilter(
            settingsFilterViewModel,
            packageListViewModel,
            isFilterDialogShown
        ) { isShown ->
            isFilterDialogShown = isShown
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    GoBackIconButton(
                        navController = navController,
                        onClick = { navController.popBackStack() }
                    )
                },
                actions = {
                    if (!isLoading) {
                        when (packageListAction) {
                            Constant.PackageListAction.USER_APPS,
                            Constant.PackageListAction.SYSTEM_APPS,
                            Constant.PackageListAction.ALL_APPS,
                            Constant.PackageListAction.DISABLED_APPS ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                    PackageListFilterIcon { isShown ->
                                        isFilterDialogShown = isShown
                                    }
                            else -> {
                                PackageListSearchIcon(
                                    onQueryChange = { query ->
                                        packageListViewModel.filterByName(query)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isLoading && !isProcessing)
                when (packageListAction) {
                    null -> {}
                    Constant.PackageListAction.USER_APPS,
                    Constant.PackageListAction.SYSTEM_APPS,
                    Constant.PackageListAction.ALL_APPS,
                    Constant.PackageListAction.DISABLED_APPS -> {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    packageListViewModel.checkVisible()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckBox,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentDescription =
                                        stringResource(R.string.description_apps_all_check),
                                )
                            }
                            Spacer(modifier = Modifier.padding(vertical = 8.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    packageListViewModel.uncheckVisible()
                                }
                            ){
                                Icon(
                                    imageVector = Icons.Default.CheckBoxOutlineBlank,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentDescription =
                                        stringResource(R.string.description_apps_all_uncheck),
                                )
                            }
                            Spacer(modifier = Modifier.padding(vertical = 8.dp))
                            FloatingActionButton(
                                onClick = {
                                    if (pkgListChecked.isNotEmpty()) {
                                        val mutablePkgList = pkgListChecked.toMutableList()
                                        mutablePkgList.apply {
                                            // clear cache of app in the end to avoid issues
                                            if (contains(context.packageName)) {
                                                remove(context.packageName)
                                                // cache dir is using for log file in debug version
                                                // clean cache dir in release only
                                                if (!BuildConfig.DEBUG)
                                                    add(context.packageName)
                                            }
                                        }
                                        localBroadcastManager.sendPackageListToClearCache(
                                            mutablePkgList as ArrayList<String>)
                                    }
                                    goBack(navController)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentDescription = stringResource(id = R.string.clear_cache_btn_text)
                                )
                            }
                        }
                    }
                    Constant.PackageListAction.IGNORED_APPS -> {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentDescription = stringResource(android.R.string.cancel)
                                )
                            }
                            Spacer(modifier = Modifier.padding(vertical = 8.dp))
                            FloatingActionButton(
                                onClick = {
                                    packageListViewModel.saveListOfIgnoredApps()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentDescription = stringResource(id = R.string.save)
                                )
                            }
                        }
                    }
                    Constant.PackageListAction.CUSTOM_LIST_ADD,
                    Constant.PackageListAction.CUSTOM_LIST_EDIT -> {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentDescription = stringResource(android.R.string.cancel)
                                )
                            }
                            Spacer(modifier = Modifier.padding(vertical = 8.dp))
                            FloatingActionButton(
                                onClick = {
                                    name?.let {
                                        packageListViewModel.saveCustomListOfApps(
                                            name = name,
                                            onSave = showToastCustomListSave,
                                            onEmpty = showToastCustomListEmpty,
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentDescription = stringResource(id = R.string.save)
                                )
                            }
                        }
                    }
                }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                    )
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Column (
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = { packageListViewModel.progressValue }
                                )
                                Text(
                                    modifier = Modifier.padding(top = 16.dp),
                                    text = packageListViewModel.progressText,
                                )
                            }
                        }
                    }
                    isProcessing -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Column (
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    else -> {
                        PackageListPackageList(
                            pkgList = pkgListCurrentVisible,
                            showCacheSize = showCacheSize,
                            onAppIconClick = {
                                Toast.makeText(context,
                                    R.string.toast_package_list_item_long_click,
                                    Toast.LENGTH_SHORT).show()
                            },
                            onAppIconLongClick = { packageName ->
                                ActivityHelper.startApplicationDetailsActivity(context, packageName)
                            },
                            onPackageClick = { pkg, checked ->
                                packageListViewModel.checkPackage(pkg.name, checked)
                            }
                        )
                    }
                }
            }
        }
    )
}
