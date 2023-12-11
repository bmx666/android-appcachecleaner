package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant.Settings.FirstBoot.Companion.DEFAULT_FIRST_BOOT
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefFirstBootManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsFirstBoot",
            produceMigrations = Migration::produceMigrations
        )
        private val KEY_FIRST_BOOT = booleanPreferencesKey("first_boot")

        // Migration section
        object Migration {
            private const val SHOW_FIRST_BOOT_CONFIRMATION =
                "show_first_boot_confirmation"

            private val keysToMigrate = setOf(SHOW_FIRST_BOOT_CONFIRMATION)

            internal fun produceMigrations(context: Context) =
                listOf(
                    SharedPreferencesMigration(
                        context = context,
                        keysToMigrate = keysToMigrate,
                        sharedPreferencesName = "FirstBoot",
                        shouldRunMigration = { true },
                        migrate = this::migrate
                    )
                )

            private fun migrate(
                sharedPreferencesView: SharedPreferencesView,
                currentData: Preferences): Preferences
            {
                return currentData.toMutablePreferences().apply {
                    val firstBoot = sharedPreferencesView.getBoolean(
                        SHOW_FIRST_BOOT_CONFIRMATION,
                        DEFAULT_FIRST_BOOT)
                    this[KEY_FIRST_BOOT] = firstBoot
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    val firstBoot: Flow<Boolean> = dataStore.data.getValue(KEY_FIRST_BOOT, DEFAULT_FIRST_BOOT)

    suspend fun setFirstBootCompleted() = dataStore.setValue(KEY_FIRST_BOOT, false)
}