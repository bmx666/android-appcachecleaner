package com.github.bmx666.appcachecleaner.ui.compose.settings

import android.os.Build
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsEditText
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.viewmodel.LocaleViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraSearchTextViewModel

@Composable
internal fun SettingsScreenExtraSearchText(
    localeViewModel: LocaleViewModel,
) {
    val settingsExtraSearchTextViewModel: SettingsExtraSearchTextViewModel = hiltViewModel()

    val clearCacheLabel = stringResource(id = R.string.clear_cache_btn_text)
    val storageLabel = stringResource(id = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            R.string.storage_settings_for_app
        else ->
            R.string.storage_label
    })
    val forceStopLabel = stringResource(id = R.string.force_stop)
    val clearDataLabel = stringResource(id = R.string.clear_user_data_text)

    val currentLocale by localeViewModel.currentLocale.collectAsState()
    val displayLanguage = currentLocale.displayLanguage
    val displayCountry = currentLocale.displayCountry

    val clearCacheSummary by settingsExtraSearchTextViewModel.getClearCache(currentLocale).collectAsState()
    val storageSummary by settingsExtraSearchTextViewModel.getStorage(currentLocale).collectAsState()
    val forceStopSummary by settingsExtraSearchTextViewModel.getForceStop(currentLocale).collectAsState()
    val clearDataSummary by settingsExtraSearchTextViewModel.getClearData(currentLocale).collectAsState()

    val clearCacheDialogLabel = stringResource(
        id = R.string.dialog_extra_search_text_message,
        displayLanguage,
        displayCountry,
        clearCacheLabel
    )
    val storageDialogLabel = stringResource(
        id = R.string.dialog_extra_search_text_message,
        displayLanguage,
        displayCountry,
        storageLabel
    )
    val forceStopDialogLabel = stringResource(
        id = R.string.dialog_extra_search_text_message,
        displayLanguage,
        displayCountry,
        forceStopLabel
    )
    val clearDataDialogLabel = stringResource(
        id = R.string.dialog_extra_search_text_message,
        displayLanguage,
        displayCountry,
        clearDataLabel
    )

    SettingsGroup(resId = R.string.btn_add_extra_search_text) {
        SettingsEditText(
            name = clearCacheLabel,
            summary = clearCacheSummary,
            dialogTextLabel = clearCacheDialogLabel,
            dialogTextPlaceholder = {
                Text(text = clearCacheLabel)
            },
            state = settingsExtraSearchTextViewModel.getClearCache(currentLocale).collectAsState(),
            onSave = { str ->
                if (str.isBlank())
                    settingsExtraSearchTextViewModel.removeClearCache(currentLocale)
                else
                    settingsExtraSearchTextViewModel.setClearCache(currentLocale, str)
            },
            onCheck = { _ -> true },
        )
        HorizontalDivider()
        SettingsEditText(
            name = storageLabel,
            summary = storageSummary,
            dialogTextLabel = storageDialogLabel,
            dialogTextPlaceholder = {
                Text(text = storageLabel)
            },
            state = settingsExtraSearchTextViewModel.getStorage(currentLocale).collectAsState(),
            onSave = { str ->
                if (str.isBlank())
                    settingsExtraSearchTextViewModel.removeStorage(currentLocale)
                else
                    settingsExtraSearchTextViewModel.setStorage(currentLocale, str)
            },
            onCheck = { _ -> true },
        )
        HorizontalDivider()
        SettingsEditText(
            name = forceStopLabel,
            summary = forceStopSummary,
            dialogTextLabel = forceStopDialogLabel,
            dialogTextPlaceholder = {
                Text(text = forceStopLabel)
            },
            state = settingsExtraSearchTextViewModel.getForceStop(currentLocale).collectAsState(),
            onSave = { str ->
                if (str.isBlank())
                    settingsExtraSearchTextViewModel.removeForceStop(currentLocale)
                else
                    settingsExtraSearchTextViewModel.setForceStop(currentLocale, str)
            },
            onCheck = { _ -> true },
        )
        HorizontalDivider()
        SettingsEditText(
            name = clearDataLabel,
            summary = clearDataSummary,
            dialogTextLabel = clearDataDialogLabel,
            dialogTextPlaceholder = {
                Text(text = clearDataLabel)
            },
            state = settingsExtraSearchTextViewModel.getClearData(currentLocale).collectAsState(),
            onSave = { str ->
                if (str.isBlank())
                    settingsExtraSearchTextViewModel.removeClearData(currentLocale)
                else
                    settingsExtraSearchTextViewModel.setClearData(currentLocale, str)
            },
            onCheck = { _ -> true },
        )
    }
}