package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.toggle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefExtraManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsExtra",
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
        )
        private val KEY_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS =
            booleanPreferencesKey("show_button_clean_cache_disabled_apps")
        private val KEY_SHOW_BUTTON_START_STOP_SERVICE =
            booleanPreferencesKey("show_button_start_stop_service")
        private val KEY_SHOW_BUTTON_CLOSE_APP =
            booleanPreferencesKey("show_button_close_app")
        private val KEY_ACTION_STOP_SERVICE =
            booleanPreferencesKey("action_stop_service")
        private val KEY_ACTION_CLOSE_APP =
            booleanPreferencesKey("action_close_app")
    }

    private val dataStore = context.dataStore

    val showButtonCleanCacheDisabledApps: Flow<Boolean> = dataStore.data.getValue(
        KEY_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS,
        Constant.Settings.Extra.DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS
    )
    val showButtonStartStopService: Flow<Boolean> = dataStore.data.getValue(
        KEY_SHOW_BUTTON_START_STOP_SERVICE,
        Constant.Settings.Extra.DEFAULT_SHOW_BUTTON_START_STOP_SERVICE
    )

    val showButtonCloseApp: Flow<Boolean> = dataStore.data.getValue(
        KEY_SHOW_BUTTON_CLOSE_APP, Constant.Settings.Extra.DEFAULT_SHOW_BUTTON_CLOSE_APP
    )

    val actionStopService: Flow<Boolean> = dataStore.data.getValue(
        KEY_ACTION_STOP_SERVICE, Constant.Settings.Extra.DEFAULT_ACTION_STOP_SERVICE
    )

    val actionCloseApp: Flow<Boolean> = dataStore.data.getValue(
        KEY_ACTION_CLOSE_APP, Constant.Settings.Extra.DEFAULT_ACTION_CLOSE_APP
    )

    suspend fun toggleShowCleanCacheDisabledApps() = dataStore.toggle(
        KEY_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS,
        Constant.Settings.Extra.DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS
    )

    suspend fun toggleShowButtonStartStopService() = dataStore.toggle(
        KEY_SHOW_BUTTON_START_STOP_SERVICE,
        Constant.Settings.Extra.DEFAULT_SHOW_BUTTON_START_STOP_SERVICE
    )

    suspend fun toggleShowButtonCloseApp() = dataStore.toggle(
        KEY_SHOW_BUTTON_CLOSE_APP, Constant.Settings.Extra.DEFAULT_SHOW_BUTTON_CLOSE_APP
    )

    suspend fun toggleActionStopService() = dataStore.toggle(
        KEY_ACTION_STOP_SERVICE, Constant.Settings.Extra.DEFAULT_ACTION_STOP_SERVICE
    )

    suspend fun toggleActionCloseApp() = dataStore.toggle(
        KEY_ACTION_CLOSE_APP, Constant.Settings.Extra.DEFAULT_ACTION_CLOSE_APP
    )
}