package com.github.bmx666.appcachecleaner.ui.compose.settings

import android.os.Build
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsEditText
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsExtraSearchTextViewModel
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import kotlinx.coroutines.runBlocking

@Composable
internal fun SettingsScreenExtraSearchText() {
    val context = LocalContext.current
    val locale = runBlocking { LocaleHelper.getCurrentLocale(context) }
    val settingsExtraSearchTextViewModel: SettingsExtraSearchTextViewModel = hiltViewModel()

    val clearCacheLabel = stringResource(id = R.string.clear_cache_btn_text)
    val storageLabel = stringResource(id = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            R.string.storage_settings_for_app
        else ->
            R.string.storage_label
    })

    SettingsGroup(resId = R.string.btn_add_extra_search_text) {
        SettingsEditText(
            name = clearCacheLabel,
            dialogTextLabel = stringResource(
                id = R.string.dialog_extra_search_text_message,
                locale.displayLanguage,
                locale.displayCountry,
                clearCacheLabel
            ),
            dialogTextPlaceholder = {
                Text(text = clearCacheLabel)
            },
            state = settingsExtraSearchTextViewModel
                .getClearCache(locale = locale).collectAsState(),
            onSave = { str ->
                if (str.isBlank())
                    settingsExtraSearchTextViewModel.removeClearCache(locale)
                else
                    settingsExtraSearchTextViewModel.setClearCache(locale, str)
            },
            onCheck = { _ -> true },
        )
        HorizontalDivider()
        SettingsEditText(
            name = storageLabel,
            dialogTextLabel = stringResource(
                id = R.string.dialog_extra_search_text_message,
                locale.displayLanguage,
                locale.displayCountry,
                storageLabel
            ),
            dialogTextPlaceholder = {
                Text(text = storageLabel)
            },
            state = settingsExtraSearchTextViewModel
                .getStorage(locale = locale).collectAsState(),
            onSave = { str ->
                if (str.isBlank())
                    settingsExtraSearchTextViewModel.removeStorage(locale)
                else
                    settingsExtraSearchTextViewModel.setStorage(locale, str)
            },
            onCheck = { _ -> true },
        )
    }
}