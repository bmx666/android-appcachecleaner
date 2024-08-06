package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.removeValue
import com.github.bmx666.appcachecleaner.util.setValue
import com.github.bmx666.appcachecleaner.util.toggle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefFilterManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsFilter",
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
        )
        private val KEY_MIN_CACHE_SIZE_BYTES =
            longPreferencesKey("min_cache_size_bytes")
        private val KEY_HIDE_DISABLED_APPS =
            booleanPreferencesKey("hide_disabled_apps")
        private val KEY_HIDE_IGNORED_APPS =
            booleanPreferencesKey("hide_ignored_apps")
        private val KEY_LIST_OF_IGNORED_APPS =
            stringSetPreferencesKey("list_of_ignored_apps")
    }

    private val dataStore = context.dataStore

    val minCacheSizeBytes: Flow<Long> = dataStore.data.getValue(
        KEY_MIN_CACHE_SIZE_BYTES, 0L
    )

    val hideDisabledApps: Flow<Boolean> = dataStore.data.getValue(
        KEY_HIDE_DISABLED_APPS, Constant.Settings.Filter.DEFAULT_HIDE_DISABLED_APPS
    )

    val hideIgnoredApps: Flow<Boolean> = dataStore.data.getValue(
        KEY_HIDE_IGNORED_APPS, Constant.Settings.Filter.DEFAULT_HIDE_IGNORED_APPS
    )

    val listOfIgnoredApps: Flow<Set<String>> = dataStore.data.getValue(
        KEY_LIST_OF_IGNORED_APPS, emptySet()
    )

    suspend fun setMinCacheSizeBytes(value: Long) = dataStore.setValue(
        KEY_MIN_CACHE_SIZE_BYTES, value
    )

    suspend fun removeMinCacheSizeBytes() = dataStore.removeValue(
        KEY_MIN_CACHE_SIZE_BYTES
    )

    suspend fun toggleHideDisabledApps() = dataStore.toggle(
        KEY_HIDE_DISABLED_APPS, Constant.Settings.Filter.DEFAULT_HIDE_DISABLED_APPS
    )

    suspend fun toggleHideIgnoredApps() = dataStore.toggle(
        KEY_HIDE_IGNORED_APPS, Constant.Settings.Filter.DEFAULT_HIDE_IGNORED_APPS
    )

    suspend fun setListOfIgnoredApps(value: Set<String>) = dataStore.setValue(
        KEY_LIST_OF_IGNORED_APPS, value
    )

    suspend fun removeListOfIgnoredApps() = dataStore.removeValue(
        KEY_LIST_OF_IGNORED_APPS
    )
}