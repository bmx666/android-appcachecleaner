package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.util.getEnumValue
import com.github.bmx666.appcachecleaner.util.setEnumValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefScenarioManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsScenario",
            produceMigrations = Migration::produceMigrations
        )
        private val KEY_SCENARIO = stringPreferencesKey("scenario")

        // Migration section
        object Migration {
            private const val SETTINGS_SCENARIO =
                "settings_scenario"

            private val keysToMigrate = setOf(SETTINGS_SCENARIO)

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
                    // old settings stored ordinal int value as string
                    // Find the matching enum by ordinal
                    val scenario = sharedPreferencesView.getString(
                        SETTINGS_SCENARIO,
                        Constant.Scenario.DEFAULT.ordinal.toString())
                        ?.toIntOrNull()
                        ?.let { ordinal ->
                            Constant.Scenario.entries.getOrNull(ordinal)
                        } ?: Constant.Scenario.DEFAULT

                    this[KEY_SCENARIO] = scenario.toString()
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    val scenario: Flow<Constant.Scenario> = dataStore.data.getEnumValue(
        KEY_SCENARIO, Constant.Scenario.DEFAULT
    )

    suspend fun setScenario(value: Constant.Scenario) = dataStore.setEnumValue(
        KEY_SCENARIO, value
    )
}