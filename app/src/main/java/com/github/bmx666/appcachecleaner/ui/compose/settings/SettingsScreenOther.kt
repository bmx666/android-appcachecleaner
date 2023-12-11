package com.github.bmx666.appcachecleaner.ui.compose.settings

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_CLEAR_CACHE_BUTTON_MS
import com.github.bmx666.appcachecleaner.ui.compose.view.ExposedDropdownMenu
import com.github.bmx666.appcachecleaner.ui.compose.view.SettingsGroup
import com.github.bmx666.appcachecleaner.ui.compose.view.StepSlider
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsScenarioViewModel
import com.github.bmx666.appcachecleaner.ui.viewmodel.SettingsTimeoutViewModel
import com.github.bmx666.appcachecleaner.util.nearest
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenOther() {
    val settingsScenarioViewModel: SettingsScenarioViewModel = hiltViewModel()
    val settingsTimeoutViewModel: SettingsTimeoutViewModel = hiltViewModel()

    val delayForNextAppTimeout by settingsTimeoutViewModel
        .delayForNextAppTimeout.collectAsState()
    val maxWaitAppTimeout by settingsTimeoutViewModel
        .maxWaitAppTimeout.collectAsState()
    val maxWaitClearCacheButtonTimeout by settingsTimeoutViewModel
        .maxWaitClearCacheButtonTimeout.collectAsState()
    val maxWaitAccessibilityEventTimeout by settingsTimeoutViewModel
        .maxWaitAccessibilityEventTimeout.collectAsState()
    val maxGoBackAfterApps by settingsTimeoutViewModel
        .maxGoBackAfterApps.collectAsState()

    val effectiveMaxWaitAppTimeout = maxWaitAppTimeout ?: MAX_WAIT_APP_PERFORM_CLICK_MS

    SettingsGroup(resId = R.string.prefs_other) {

        ExposedDropdownMenu(
            state = settingsScenarioViewModel.scenario,
            options = Constant.Scenario.entries,
            optionsNames = stringArrayResource(id = R.array.settings_scenario_entries),
            label = stringResource(id = R.string.prefs_title_scenario),
            onValueChangedEvent = { scenario ->
                settingsScenarioViewModel.setScenario(scenario)
            },
        )

        StepSlider(
            onLabelUpdate = { value ->
                stringResource(id = R.string.prefs_title_delay_for_next_app_timeout,
                    nearest(value, 250f) / 1000f)
            },
            onSummaryUpdate = { _ ->
                stringResource(id = R.string.prefs_summary_delay_for_next_app_timeout)
            },
            value = delayForNextAppTimeout,
            valueRange = MIN_DELAY_FOR_NEXT_APP_MS.toFloat()..MAX_DELAY_FOR_NEXT_APP_MS.toFloat(),
            onValueChangeFinished = { value ->
                settingsTimeoutViewModel.setDelayForNextAppTimeout(
                    nearest(value, 250f).roundToInt())
            }
        )

        StepSlider(
            onLabelUpdate = { value ->
                stringResource(id = R.string.prefs_title_max_wait_app_timeout,
                    nearest(value, 250f) / 1000f)
            },
            onSummaryUpdate = { _ ->
                stringResource(id = R.string.prefs_summary_max_wait_app_timeout)
            },
            value = maxWaitAppTimeout,
            valueRange = MIN_WAIT_APP_PERFORM_CLICK_MS.toFloat()..MAX_WAIT_APP_PERFORM_CLICK_MS.toFloat(),
            onValueChangeFinished = { value ->
                settingsTimeoutViewModel.setMaxWaitAppTimeout(
                    nearest(value, 250f).roundToInt())
            }
        )

        StepSlider(
            onLabelUpdate = { value ->
                stringResource(id = R.string.prefs_title_max_wait_clear_cache_btn_timeout,
                    nearest(value, 250f) / 1000f)
            },
            onSummaryUpdate = { _ ->
                stringResource(id = R.string.prefs_summary_max_wait_clear_cache_btn_timeout)
            },
            value = maxWaitClearCacheButtonTimeout,
            valueRange = MIN_WAIT_CLEAR_CACHE_BUTTON_MS.toFloat()..effectiveMaxWaitAppTimeout.toFloat(),
            onValueChangeFinished = { value ->
                settingsTimeoutViewModel.setMaxWaitClearCacheButtonTimeout(
                    nearest(value, 250f).roundToInt())
            }
        )

        StepSlider(
            onLabelUpdate = { value ->
                stringResource(id = R.string.prefs_title_max_wait_accessibility_event_timeout,
                    nearest(value, 250f) / 1000f)
            },
            onSummaryUpdate = { _ ->
                stringResource(id = R.string.prefs_summary_max_wait_accessibility_event_timeout)
            },
            value = maxWaitAccessibilityEventTimeout,
            valueRange = MIN_WAIT_ACCESSIBILITY_EVENT_MS.toFloat()..MAX_WAIT_ACCESSIBILITY_EVENT_MS.toFloat(),
            onValueChangeFinished = { value ->
                settingsTimeoutViewModel.setMaxWaitAccessibilityEventTimeout(
                    nearest(value, 250f).roundToInt())
            }
        )

        StepSlider(
            onLabelUpdate = { value ->
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                        stringResource(id = R.string.prefs_title_go_back_after_apps, value.roundToInt())
                    value.roundToInt() == MIN_GO_BACK_AFTER_APPS ->
                        stringResource(id = R.string.prefs_title_go_back_after_apps_never)
                    else ->
                        stringResource(id = R.string.prefs_title_go_back_after_apps, value.roundToInt())
                }
            },
            onSummaryUpdate = { _ ->
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                        stringResource(id = R.string.prefs_summary_go_back_after_apps_for_api_34)
                    else ->
                        stringResource(id = R.string.prefs_summary_go_back_after_apps)
                }
            },
            value = maxGoBackAfterApps,
            valueRange = MIN_GO_BACK_AFTER_APPS.toFloat()..MAX_GO_BACK_AFTER_APPS.toFloat(),
            onValueChangeFinished = { value ->
                settingsTimeoutViewModel.setMaxGoBackAfterApps(value.roundToInt())
            },
        )
    }
}