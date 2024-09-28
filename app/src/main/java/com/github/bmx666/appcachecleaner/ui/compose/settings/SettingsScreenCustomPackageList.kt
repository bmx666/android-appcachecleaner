package com.github.bmx666.appcachecleaner.ui.compose.settings

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsCustomListAdd
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsCustomListEdit
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsCustomListRemove
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsCustomPackageListViewModel
import com.github.bmx666.appcachecleaner.util.encode

@Composable
internal fun SettingsScreenCustomPackageList(navController: NavController) {
    val context = LocalContext.current
    val settingsCustomPackageListViewModel: SettingsCustomPackageListViewModel = hiltViewModel()
    val listNames by settingsCustomPackageListViewModel.listNames.collectAsState()

    val showToastListRemove: (String) -> Unit = { name ->
        Toast.makeText(context,
            context.getString(R.string.toast_custom_list_has_been_removed, name),
            Toast.LENGTH_SHORT).show()
    }

    SettingsGroup(resId = R.string.prefs_custom_lists) {
        SettingsCustomListAdd(
            imageVector = Icons.Default.Add,
            title = stringResource(id = R.string.prefs_title_custom_list_add),
            summary = stringResource(id = R.string.prefs_summary_custom_list_add),
            dialogTextLabel = stringResource(
                id = R.string.dialog_message_custom_list_add),
            state = settingsCustomPackageListViewModel.listNames,
            onSave = { name ->
                val root = Constant.Navigation.PACKAGE_LIST
                val action = Constant.PackageListAction.CUSTOM_LIST_ADD
                navController.navigate(
                    route = "$root/$action?name=${name.encode()}"
                )
            },
        )

        if (!listNames.isNullOrEmpty()) {
            HorizontalDivider()
            SettingsCustomListEdit(
                imageVector = Icons.Default.Edit,
                title = stringResource(id = R.string.prefs_title_custom_list_edit),
                summary = stringResource(id = R.string.prefs_summary_custom_list_edit),
                dialogTextLabel = stringResource(
                    id = R.string.dialog_message_custom_list_edit),
                state = settingsCustomPackageListViewModel.listNames,
                onSave = { name ->
                    val root = Constant.Navigation.PACKAGE_LIST
                    val action = Constant.PackageListAction.CUSTOM_LIST_EDIT
                    navController.navigate(
                        route = "$root/$action?name=${name.encode()}"
                    )
                },
            )
            HorizontalDivider()
            SettingsCustomListRemove(
                imageVector = Icons.Default.Delete,
                title = stringResource(id = R.string.prefs_title_custom_list_remove),
                summary = stringResource(id = R.string.prefs_summary_custom_list_remove),
                dialogTextLabel = stringResource(
                    id = R.string.dialog_message_custom_list_remove),
                state = settingsCustomPackageListViewModel.listNames,
                onSave = { str ->
                    settingsCustomPackageListViewModel.removeCustomPackageList(str)
                    showToastListRemove(str)
                },
            )
        }
    }
}