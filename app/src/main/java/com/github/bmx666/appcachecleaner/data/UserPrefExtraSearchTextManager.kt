package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.util.getValueOrNull
import com.github.bmx666.appcachecleaner.util.removeValue
import com.github.bmx666.appcachecleaner.util.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject

class UserPrefExtraSearchTextManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsExtraSearchText",
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
        )
        private const val KEY_CLEAR_CACHE = "clear_cache"
        private const val KEY_STORAGE = "storage"
    }

    private val dataStore = context.dataStore

    fun getClearCache(locale: Locale): Flow<String?> =
        dataStore.data.getValueOrNull(
            stringPreferencesKey("$locale,$KEY_CLEAR_CACHE")
        )

    suspend fun setClearCache(locale: Locale, value: CharSequence?) {
        if (value.isNullOrEmpty() or value!!.trim().isEmpty()) return
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
        if (value.isNullOrEmpty() or value!!.trim().isEmpty()) return
        dataStore.setValue(
            stringPreferencesKey("$locale,$KEY_STORAGE"), value.toString()
        )
    }

    suspend fun removeStorage(locale: Locale) {
        dataStore.removeValue(
            stringPreferencesKey("$locale,$KEY_STORAGE")
        )
    }
}