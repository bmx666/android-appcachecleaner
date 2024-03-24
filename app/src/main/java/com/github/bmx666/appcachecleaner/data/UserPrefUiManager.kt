package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant.Settings.UI.Companion.DEFAULT_DYNAMIC_COLOR
import com.github.bmx666.appcachecleaner.const.Constant.Settings.UI.Companion.DEFAULT_FORCE_NIGHT_MODE
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.toggle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefUiManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsUi",
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
        )
        val KEY_NIGHT_MODE = booleanPreferencesKey("night_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    private val dataStore = context.dataStore

    val forceNightMode: Flow<Boolean> =
        dataStore.data.getValue(KEY_NIGHT_MODE, DEFAULT_FORCE_NIGHT_MODE)

    val dynamicColor: Flow<Boolean> =
        dataStore.data.getValue(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)

    suspend fun toggleForceNightMode() = dataStore.toggle(KEY_NIGHT_MODE, DEFAULT_FORCE_NIGHT_MODE)

    suspend fun toggleDynamicColor() = dataStore.toggle(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)
}