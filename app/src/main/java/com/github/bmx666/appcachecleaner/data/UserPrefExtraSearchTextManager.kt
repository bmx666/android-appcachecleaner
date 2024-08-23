package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.util.getValueOrNull
import com.github.bmx666.appcachecleaner.util.removeValue
import com.github.bmx666.appcachecleaner.util.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject

class UserPrefExtraSearchTextManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsExtraSearchText",
            produceMigrations = Migration::produceMigrations
        )
        private const val KEY_CLEAR_CACHE = "clear_cache"
        private const val KEY_STORAGE = "storage"
        private const val KEY_FORCE_STOP = "force_stop"

        // Migration section
        object Migration {
            internal fun produceMigrations(context: Context) =
                listOf(
                    SharedPreferencesMigration(
                        context = context,
                        sharedPreferencesName = "ExtraSearchText",
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
                        val value = sharedPreferencesView.getString(key)
                        if (value?.isNotEmpty() == true)
                            this[stringPreferencesKey(key)] = value
                    }
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    fun getClearCache(locale: Locale): Flow<String?> =
        dataStore.data.getValueOrNull(
            stringPreferencesKey("$locale,$KEY_CLEAR_CACHE")
        )

    suspend fun setClearCache(locale: Locale, value: CharSequence?) {
        if (value.isNullOrBlank()) return
        dataStore.setValue(
            stringPreferencesKey("$locale,$KEY_CLEAR_CACHE"), value.toString()
        )
    }

    suspend fun removeClearCache(locale: Locale) {
        dataStore.removeValue(
            stringPreferencesKey("$locale,$KEY_CLEAR_CACHE")
        )
    }

    fun getStorage(locale: Locale): Flow<String?> =
        dataStore.data.getValueOrNull(
            stringPreferencesKey("$locale,$KEY_STORAGE")
        )

    suspend fun setStorage(locale: Locale, value: CharSequence?) {
        if (value.isNullOrBlank()) return
        dataStore.setValue(
            stringPreferencesKey("$locale,$KEY_STORAGE"), value.toString()
        )
    }

    suspend fun removeStorage(locale: Locale) {
        dataStore.removeValue(
            stringPreferencesKey("$locale,$KEY_STORAGE")
        )
    }

    fun getForceStop(locale: Locale): Flow<String?> =
        dataStore.data.getValueOrNull(
            stringPreferencesKey("$locale,$KEY_FORCE_STOP")
        )

    suspend fun setForceStop(locale: Locale, value: CharSequence?) {
        if (value.isNullOrBlank()) return
        dataStore.setValue(
            stringPreferencesKey("$locale,$KEY_FORCE_STOP"), value.toString()
        )
    }

    suspend fun removeForceStop(locale: Locale) {
        dataStore.removeValue(
            stringPreferencesKey("$locale,$KEY_FORCE_STOP")
        )
    }
}