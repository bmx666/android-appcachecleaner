package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefTimeoutManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsSettings",
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
        )
        val KEY_DELAY_FOR_NEXT_APP_TIMEOUT =
            intPreferencesKey("delay_for_next_app_timeout")
        val KEY_MAX_WAIT_APP_TIMEOUT =
            intPreferencesKey("max_wait_app_timeout")
        val KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT =
            intPreferencesKey("max_wait_clear_cache_btn_timeout")
    }

    private val dataStore = context.dataStore

    val delayForNextAppTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_DELAY_FOR_NEXT_APP_TIMEOUT,
        Constant.Settings.CacheClean.DEFAULT_DELAY_FOR_NEXT_APP_MS / 1000
    )

    val maxWaitAppTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_APP_TIMEOUT,
        Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS / 1000
    )

    val maxWaitClearCacheButtonTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT,
        Constant.Settings.CacheClean.DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS / 1000
    )

    suspend fun setDelayForNextAppTimeout(value: Int) = dataStore.setValue(
        KEY_DELAY_FOR_NEXT_APP_TIMEOUT, value
    )

    suspend fun setMaxWaitAppTimeout(value: Int) = dataStore.setValue(
        KEY_MAX_WAIT_APP_TIMEOUT, value
    )

    suspend fun setMaxWaitClearCacheButtonTimeout(value: Int) = dataStore.setValue(
        KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT, value
    )
}