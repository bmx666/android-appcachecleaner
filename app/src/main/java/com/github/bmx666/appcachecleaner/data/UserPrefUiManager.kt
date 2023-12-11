package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant.Settings.UI.Companion.DEFAULT_CONTRAST
import com.github.bmx666.appcachecleaner.const.Constant.Settings.UI.Companion.DEFAULT_DYNAMIC_COLOR
import com.github.bmx666.appcachecleaner.const.Constant.Settings.UI.Companion.DEFAULT_FORCE_NIGHT_MODE
import com.github.bmx666.appcachecleaner.const.Constant.Settings.UI.Contrast
import com.github.bmx666.appcachecleaner.util.getEnumValue
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.setEnumValue
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
            produceMigrations = Migration::produceMigrations
        )
        private val KEY_NIGHT_MODE = booleanPreferencesKey("night_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_CONTRAST = stringPreferencesKey("contrast")

        // Migration section
        object Migration {
            private const val UI_NIGHT_MODE =
                "ui_night_mode"

            private val keysToMigrate = setOf(UI_NIGHT_MODE)

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
                    val nightMode = sharedPreferencesView.getBoolean(
                        UI_NIGHT_MODE,
                        DEFAULT_FORCE_NIGHT_MODE)
                    this[KEY_NIGHT_MODE] = nightMode
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    val forceNightMode: Flow<Boolean> =
        dataStore.data.getValue(KEY_NIGHT_MODE, DEFAULT_FORCE_NIGHT_MODE)

    val dynamicColor: Flow<Boolean> =
        dataStore.data.getValue(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)

    val contrast: Flow<Contrast> =
        dataStore.data.getEnumValue(KEY_CONTRAST, DEFAULT_CONTRAST)

    suspend fun toggleForceNightMode() = dataStore.toggle(KEY_NIGHT_MODE, DEFAULT_FORCE_NIGHT_MODE)

    suspend fun toggleDynamicColor() = dataStore.toggle(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)

    suspend fun setContrast(contrast: Contrast) = dataStore.setEnumValue(KEY_CONTRAST, contrast)
}