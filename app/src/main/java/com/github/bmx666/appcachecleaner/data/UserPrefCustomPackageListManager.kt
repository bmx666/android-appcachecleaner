package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.util.getValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefCustomPackageListManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsCustomPackageList",
            produceMigrations = Migration::produceMigrations
        )
        private val KEY_LIST_NAMES = stringSetPreferencesKey("list_names")

        // Migration section
        object Migration {
            internal fun produceMigrations(context: Context) =
                listOf(
                    SharedPreferencesMigration(
                        context = context,
                        sharedPreferencesName = "package-list",
                        shouldRunMigration = { true },
                        migrate = Migration::migrate
                    )
                )

            private fun migrate(
                sharedPreferencesView: SharedPreferencesView,
                currentData: Preferences): Preferences
            {
                return currentData.toMutablePreferences().apply {
                    val listNames = sharedPreferencesView.getStringSet("list_names") ?: emptySet()
                    val newListNames = listNames.toMutableSet()
                    listNames.filter { name -> name != "list_names" }.forEach { name ->
                        val pkgList = sharedPreferencesView.getStringSet(name)
                        if (pkgList?.isNotEmpty() == true) {
                            newListNames.add(name)
                            this[KEY_LIST_NAMES] = newListNames
                            this[stringSetPreferencesKey(name)] = pkgList
                        }
                    }
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    val listNames: Flow<Set<String>> = dataStore.data.getValue(
        KEY_LIST_NAMES, emptySet()
    )

    fun getCustomList(name: String): Flow<Set<String>> = dataStore.data.getValue(
        stringSetPreferencesKey(name), emptySet()
    )

    suspend fun setCustomList(name: String, value: Set<String>) {
        dataStore.edit { preferences ->
            val currentListNames = preferences[KEY_LIST_NAMES] ?: emptySet()
            val updatedListNames = currentListNames.toMutableSet()
            updatedListNames.add(name)
            preferences[KEY_LIST_NAMES] = updatedListNames
            preferences[stringSetPreferencesKey(name)] = value
        }
    }

    suspend fun removeCustomList(name: String) {
        dataStore.edit { preferences ->
            val currentListNames = preferences[KEY_LIST_NAMES] ?: emptySet()
            val updatedListNames = currentListNames.toMutableSet()
            updatedListNames.remove(name)
            preferences[KEY_LIST_NAMES] = updatedListNames
            preferences.remove(stringSetPreferencesKey(name))
        }
    }
}