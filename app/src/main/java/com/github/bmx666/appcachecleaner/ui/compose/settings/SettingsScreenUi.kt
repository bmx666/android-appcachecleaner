package com.github.bmx666.appcachecleaner.ui.compose.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.compose.view.IconText
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsSwitch
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsUiViewModel

@Composable
internal fun SettingsScreenUi() {
    val settingsUiViewModel: SettingsUiViewModel = hiltViewModel()

    val currentContrast by settingsUiViewModel.contrast.collectAsState()
    val currentDynamicColor by settingsUiViewModel.dynamicColor.collectAsState()
    val isDynamicColorEnabled =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && currentDynamicColor == true

    SettingsGroup(resId = R.string.prefs_ui) {
        SettingsSwitch(
            titleResId = R.string.prefs_ui_night_mode,
            state = settingsUiViewModel.forceNightMode.collectAsState(),
            onClick = { settingsUiViewModel.toggleForceNightMode() }
        )
        HorizontalDivider()
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .alpha(if (isDynamicColorEnabled) 0.38f else 1.0f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.prefs_ui_contrast),
                maxLines = 1,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    enabled = !isDynamicColorEnabled
                            && currentContrast != Constant.Settings.UI.Contrast.STANDARD,
                    onClick = {
                        settingsUiViewModel.setContrast(
                            Constant.Settings.UI.Contrast.STANDARD
                        )
                    }
                ) {
                    IconText(
                        iconCode = "\uE1AD",
                        modifier = Modifier.semantics {
                            contentDescription = Constant.Settings.UI.Contrast.STANDARD.name
                        }
                    )
                }
                Button(
                    enabled = !isDynamicColorEnabled
                            && currentContrast != Constant.Settings.UI.Contrast.MEDIUM,
                    onClick = {
                        settingsUiViewModel.setContrast(
                            Constant.Settings.UI.Contrast.MEDIUM
                        )
                    }
                ) {
                    IconText(
                        iconCode = "\uE3AB",
                        modifier = Modifier.semantics {
                            contentDescription = Constant.Settings.UI.Contrast.STANDARD.name
                        }
                    )
                }
                Button(
                    enabled = !isDynamicColorEnabled
                            && currentContrast != Constant.Settings.UI.Contrast.HIGH,
                    onClick = {
                        settingsUiViewModel.setContrast(
                            Constant.Settings.UI.Contrast.HIGH
                        )
                    }
                ) {
                    IconText(
                        iconCode = "\uE1AC",
                        modifier = Modifier.semantics {
                            contentDescription = Constant.Settings.UI.Contrast.STANDARD.name
                        }
                    )
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HorizontalDivider()
            SettingsSwitch(
                titleResId = R.string.prefs_ui_dynamic_color,
                state = settingsUiViewModel.dynamicColor.collectAsState(),
                onClick = { settingsUiViewModel.toggleDynamicColor() }
            )
        }
    }
}