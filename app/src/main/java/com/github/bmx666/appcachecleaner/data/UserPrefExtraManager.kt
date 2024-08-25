package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Extra.Companion.DEFAULT_ACTION_CLOSE_APP
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Extra.Companion.DEFAULT_ACTION_FORCE_STOP_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Extra.Companion.DEFAULT_ACTION_STOP_SERVICE
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Extra.Companion.DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Extra.Companion.DEFAULT_SHOW_BUTTON_CLEAR_DATA
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Extra.Companion.DEFAULT_SHOW_BUTTON_CLOSE_APP
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Extra.Companion.DEFAULT_SHOW_BUTTON_START_STOP_SERVICE
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
            produceMigrations = Migration::produceMigrations
        )
        private val KEY_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS =
            booleanPreferencesKey("show_button_clean_cache_disabled_apps")
        private val KEY_SHOW_BUTTON_START_STOP_SERVICE =
            booleanPreferencesKey("show_button_start_stop_service")
        private val KEY_SHOW_BUTTON_CLOSE_APP =
            booleanPreferencesKey("show_button_close_app")
        private val KEY_SHOW_BUTTON_CLEAR_DATA =
            booleanPreferencesKey("show_button_clear_data")
        private val KEY_ACTION_FORCE_STOP_APPS =
            booleanPreferencesKey("action_force_stop_apps")
        private val KEY_ACTION_STOP_SERVICE =
            booleanPreferencesKey("action_stop_service")
        private val KEY_ACTION_CLOSE_APP =
            booleanPreferencesKey("action_close_app")

        // Migration section
        object Migration {
            private const val SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS =
                "show_button_clean_cache_disabled_apps"
            private const val SHOW_BUTTON_START_STOP_SERVICE =
                "show_button_start_stop_service"
            private const val SHOW_BUTTON_CLOSE_APP =
                "show_button_close_app"
            private const val EXTRA_ACTION_STOP_SERVICE =
                "extra_action_stop_service"
            private const val EXTRA_ACTION_CLOSE_APP =
                "extra_action_close_app"

            private val keysToMigrate = setOf(
                SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS,
                SHOW_BUTTON_START_STOP_SERVICE,
                SHOW_BUTTON_CLOSE_APP,
                EXTRA_ACTION_STOP_SERVICE,
                EXTRA_ACTION_CLOSE_APP,
            )

            internal fun produceMigrations(context: Context) =
                listOf(
                    SharedPreferencesMigration(
                        context = context,
                        keysToMigrate = keysToMigrate,
                        sharedPreferencesName = context.packageName + "_preferences",
                        shouldRunMigration = { true },
                        migrate = this::migrate
                    )
                )

            private fun migrate(
                sharedPreferencesView: SharedPreferencesView,
                currentData: Preferences): Preferences
            {
                return currentData.toMutablePreferences().apply {
                    sharedPreferencesView.getAll().forEach { kv ->
                        val key = kv.key
                        val prefKey =
                            when (key) {
                                EXTRA_ACTION_STOP_SERVICE ->
                                    KEY_ACTION_STOP_SERVICE
                                EXTRA_ACTION_CLOSE_APP ->
                                    KEY_ACTION_CLOSE_APP
                                else -> booleanPreferencesKey(key)
                            }
                        val defValue =
                            when (key) {
                                SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS ->
                                    DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS
                                SHOW_BUTTON_START_STOP_SERVICE ->
                                    DEFAULT_SHOW_BUTTON_START_STOP_SERVICE
                                SHOW_BUTTON_CLOSE_APP ->
                                    DEFAULT_SHOW_BUTTON_CLOSE_APP
                                EXTRA_ACTION_STOP_SERVICE ->
                                    DEFAULT_ACTION_STOP_SERVICE
                                EXTRA_ACTION_CLOSE_APP ->
                                    DEFAULT_ACTION_CLOSE_APP
                                else -> false
                            }
                        this[prefKey] =
                            sharedPreferencesView.getBoolean(key, defValue)
                    }
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    val showButtonCleanCacheDisabledApps: Flow<Boolean> = dataStore.data.getValue(
        KEY_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS, DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS
    )
    val showButtonStartStopService: Flow<Boolean> = dataStore.data.getValue(
        KEY_SHOW_BUTTON_START_STOP_SERVICE, DEFAULT_SHOW_BUTTON_START_STOP_SERVICE
    )

    val showButtonCloseApp: Flow<Boolean> = dataStore.data.getValue(
        KEY_SHOW_BUTTON_CLOSE_APP, DEFAULT_SHOW_BUTTON_CLOSE_APP
    )

    val showButtonClearData: Flow<Boolean> = dataStore.data.getValue(
        KEY_SHOW_BUTTON_CLEAR_DATA, DEFAULT_SHOW_BUTTON_CLEAR_DATA
    )

    val actionForceStopApps: Flow<Boolean> = dataStore.data.getValue(
        KEY_ACTION_FORCE_STOP_APPS, DEFAULT_ACTION_FORCE_STOP_APPS
    )

    val actionStopService: Flow<Boolean> = dataStore.data.getValue(
        KEY_ACTION_STOP_SERVICE, DEFAULT_ACTION_STOP_SERVICE
    )

    val actionCloseApp: Flow<Boolean> = dataStore.data.getValue(
        KEY_ACTION_CLOSE_APP, DEFAULT_ACTION_CLOSE_APP
    )

    suspend fun toggleShowCleanCacheDisabledApps() = dataStore.toggle(
        KEY_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS, DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS
    )

    suspend fun toggleShowButtonStartStopService() = dataStore.toggle(
        KEY_SHOW_BUTTON_START_STOP_SERVICE, DEFAULT_SHOW_BUTTON_START_STOP_SERVICE
    )

    suspend fun toggleShowButtonCloseApp() = dataStore.toggle(
        KEY_SHOW_BUTTON_CLOSE_APP, DEFAULT_SHOW_BUTTON_CLOSE_APP
    )

    suspend fun toggleShowButtonClearData() = dataStore.toggle(
        KEY_SHOW_BUTTON_CLEAR_DATA, DEFAULT_SHOW_BUTTON_CLEAR_DATA
    )

    suspend fun toggleActionForceStopApps() = dataStore.toggle(
        KEY_ACTION_FORCE_STOP_APPS, DEFAULT_ACTION_FORCE_STOP_APPS
    )

    suspend fun toggleActionStopService() = dataStore.toggle(
        KEY_ACTION_STOP_SERVICE, DEFAULT_ACTION_STOP_SERVICE
    )

    suspend fun toggleActionCloseApp() = dataStore.toggle(
        KEY_ACTION_CLOSE_APP, DEFAULT_ACTION_CLOSE_APP
    )
}