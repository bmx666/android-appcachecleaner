package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Filter.Companion.DEFAULT_HIDE_DISABLED_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.Filter.Companion.DEFAULT_HIDE_IGNORED_APPS
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
            produceMigrations = Migration::produceMigrations
        )
        private val KEY_MIN_CACHE_SIZE_BYTES =
            longPreferencesKey("min_cache_size_bytes")
        private val KEY_HIDE_DISABLED_APPS =
            booleanPreferencesKey("hide_disabled_apps")
        private val KEY_HIDE_IGNORED_APPS =
            booleanPreferencesKey("hide_ignored_apps")
        private val KEY_LIST_OF_IGNORED_APPS =
            stringSetPreferencesKey("list_of_ignored_apps")

        // Migration section
        object Migration {
            private const val FILTER_MIN_CACHE_SIZE_BYTES =
                "filter_min_cache_size_bytes"
            private const val FILTER_HIDE_DISABLED_APPS =
                "filter_hide_disabled_apps"
            private const val FILTER_HIDE_IGNORED_APPS =
                "filter_hide_ignored_apps"
            private const val FILTER_LIST_OF_IGNORED_APPS =
                "filter_list_of_ignored_apps"
            private const val FILTER_SHOW_DIALOG_TO_IGNORE_APP =
                "filter_show_dialog_to_ignore_app"

            private val keysToMigrate = setOf(
                FILTER_MIN_CACHE_SIZE_BYTES,
                FILTER_HIDE_DISABLED_APPS,
                FILTER_HIDE_IGNORED_APPS,
                FILTER_LIST_OF_IGNORED_APPS,
                FILTER_SHOW_DIALOG_TO_IGNORE_APP,
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
                        when (val key = kv.key) {
                            FILTER_MIN_CACHE_SIZE_BYTES ->
                                this[KEY_MIN_CACHE_SIZE_BYTES] =
                                    sharedPreferencesView.getLong(key, 0L)
                            FILTER_HIDE_DISABLED_APPS ->
                                this[KEY_HIDE_DISABLED_APPS] =
                                    sharedPreferencesView.getBoolean(
                                        key, DEFAULT_HIDE_DISABLED_APPS)
                            FILTER_HIDE_IGNORED_APPS ->
                                this[KEY_HIDE_IGNORED_APPS] =
                                    sharedPreferencesView.getBoolean(
                                        key, DEFAULT_HIDE_IGNORED_APPS)
                            FILTER_LIST_OF_IGNORED_APPS ->
                                this[KEY_LIST_OF_IGNORED_APPS] =
                                    sharedPreferencesView.getStringSet(
                                        key, emptySet()) ?: emptySet()
                            // deprecated, ignore
                            /* FILTER_SHOW_DIALOG_TO_IGNORE_APP */
                        }
                    }
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    val minCacheSizeBytes: Flow<Long> = dataStore.data.getValue(
        KEY_MIN_CACHE_SIZE_BYTES, 0L
    )

    val hideDisabledApps: Flow<Boolean> = dataStore.data.getValue(
        KEY_HIDE_DISABLED_APPS, DEFAULT_HIDE_DISABLED_APPS
    )

    val hideIgnoredApps: Flow<Boolean> = dataStore.data.getValue(
        KEY_HIDE_IGNORED_APPS, DEFAULT_HIDE_IGNORED_APPS
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
        KEY_HIDE_DISABLED_APPS, DEFAULT_HIDE_DISABLED_APPS
    )

    suspend fun toggleHideIgnoredApps() = dataStore.toggle(
        KEY_HIDE_IGNORED_APPS, DEFAULT_HIDE_IGNORED_APPS
    )

    suspend fun setListOfIgnoredApps(value: Set<String>) = dataStore.setValue(
        KEY_LIST_OF_IGNORED_APPS, value
    )

    suspend fun removeListOfIgnoredApps() = dataStore.removeValue(
        KEY_LIST_OF_IGNORED_APPS
    )
}