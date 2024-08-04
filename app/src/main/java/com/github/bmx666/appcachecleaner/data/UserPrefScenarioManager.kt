package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
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
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
        )
        private val KEY_SCENARIO = stringPreferencesKey("scenario")
    }

    private val dataStore = context.dataStore

    val scenario: Flow<Constant.Scenario> = dataStore.data.getEnumValue(
        KEY_SCENARIO, Constant.Scenario.DEFAULT
    )

    suspend fun setScenario(value: Constant.Scenario) = dataStore.setEnumValue(
        KEY_SCENARIO, value
    )
}