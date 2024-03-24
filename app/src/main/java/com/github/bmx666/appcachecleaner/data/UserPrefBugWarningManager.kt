package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant.Settings.BugWarning.Companion.DEFAULT_SHOW_BUG_322519674
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefBugWarningManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsBugWarning",
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
        )
        val KEY_BUG_322519674 = booleanPreferencesKey("bug_322519674")
    }

    private val dataStore = context.dataStore

    val showBug322519674: Flow<Boolean> = dataStore.data.getValue(
        KEY_BUG_322519674, DEFAULT_SHOW_BUG_322519674
    )

    suspend fun hideBug322519674() = dataStore.setValue(KEY_BUG_322519674, false)
}