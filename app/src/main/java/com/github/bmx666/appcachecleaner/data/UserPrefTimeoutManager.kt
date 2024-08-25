package com.github.bmx666.appcachecleaner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_FOR_NEXT_APP_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_GO_BACK_AFTER_APPS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_CLEAR_CACHE_BUTTON_MS
import com.github.bmx666.appcachecleaner.util.clamp
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UserPrefTimeoutManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsTimeout",
            produceMigrations = Migration::produceMigrations
        )

        private val KEY_DELAY_FOR_NEXT_APP_TIMEOUT =
            intPreferencesKey("delay_for_next_app_timeout")
        private val KEY_MAX_WAIT_APP_TIMEOUT =
            intPreferencesKey("max_wait_app_timeout")
        private val KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT =
            intPreferencesKey("max_wait_clear_cache_btn_timeout")
        private val KEY_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT =
            intPreferencesKey("max_wait_accessibility_event_timeout")
        private val KEY_MAX_GO_BACK_AFTER_APPS =
            intPreferencesKey("max_go_back_after_apps")

        // Migration section
        object Migration {
            private const val SETTINGS_DELAY_FOR_NEXT_APP_TIMEOUT =
                "settings_delay_for_next_app_timeout"
            private const val SETTINGS_MAX_WAIT_APP_TIMEOUT =
                "settings_max_wait_app_timeout"
            private const val SETTINGS_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT =
                "settings_max_wait_clear_cache_btn_timeout"
            private const val SETTINGS_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT =
                "settings_max_wait_accessibility_event_timeout"
            private const val SETTINGS_GO_BACK_AFTER_APPS =
                "settings_go_back_after_apps"

            private val keysToMigrate = setOf(
                SETTINGS_DELAY_FOR_NEXT_APP_TIMEOUT,
                SETTINGS_MAX_WAIT_APP_TIMEOUT,
                SETTINGS_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT,
                SETTINGS_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT,
                SETTINGS_GO_BACK_AFTER_APPS,
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
                            SETTINGS_DELAY_FOR_NEXT_APP_TIMEOUT -> {
                                // old settings stored value in seconds
                                val value = sharedPreferencesView.getInt(key,
                                    DEFAULT_DELAY_FOR_NEXT_APP_MS / 1000) * 1000
                                this[KEY_DELAY_FOR_NEXT_APP_TIMEOUT] =
                                    clamp(value,
                                        MIN_DELAY_FOR_NEXT_APP_MS,
                                        MAX_DELAY_FOR_NEXT_APP_MS)
                            }
                            SETTINGS_MAX_WAIT_APP_TIMEOUT -> {
                                val value = sharedPreferencesView.getInt(key,
                                    DEFAULT_WAIT_APP_PERFORM_CLICK_MS / 1000) * 1000
                                // old settings stored value in seconds
                                this[KEY_MAX_WAIT_APP_TIMEOUT] =
                                    clamp(value,
                                        MIN_WAIT_APP_PERFORM_CLICK_MS,
                                        MAX_WAIT_APP_PERFORM_CLICK_MS)
                            }
                            SETTINGS_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT -> {
                                val value = sharedPreferencesView.getInt(key,
                                    DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS / 1000) * 1000
                                // old settings stored value in seconds
                                this[KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT] = value
                            }
                            SETTINGS_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT -> {
                                val value = sharedPreferencesView.getInt(key,
                                    DEFAULT_WAIT_ACCESSIBILITY_EVENT_MS / 1000) * 1000
                                // old settings stored value in seconds
                                this[KEY_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT] =
                                    clamp(value,
                                        MIN_WAIT_ACCESSIBILITY_EVENT_MS,
                                        MAX_WAIT_ACCESSIBILITY_EVENT_MS)
                            }
                            SETTINGS_GO_BACK_AFTER_APPS -> {
                                val value = sharedPreferencesView.getInt(key,
                                    DEFAULT_GO_BACK_AFTER_APPS)
                                this[KEY_MAX_GO_BACK_AFTER_APPS] =
                                    clamp(value,
                                        MIN_GO_BACK_AFTER_APPS,
                                        MAX_GO_BACK_AFTER_APPS)
                            }
                        }
                    }
                }.toPreferences()
            }
        }
    }

    private val dataStore = context.dataStore

    val delayForNextAppTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_DELAY_FOR_NEXT_APP_TIMEOUT,
        DEFAULT_DELAY_FOR_NEXT_APP_MS,
        MIN_DELAY_FOR_NEXT_APP_MS,
        MAX_DELAY_FOR_NEXT_APP_MS,
    )

    val maxWaitAppTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_APP_TIMEOUT,
        DEFAULT_WAIT_APP_PERFORM_CLICK_MS,
        MIN_WAIT_APP_PERFORM_CLICK_MS,
        MAX_WAIT_APP_PERFORM_CLICK_MS,
    )

    val maxWaitClearCacheButtonTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT,
        DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS,
        MIN_WAIT_CLEAR_CACHE_BUTTON_MS,
        MAX_WAIT_APP_PERFORM_CLICK_MS,
    )

    val maxWaitAccessibilityEventTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT,
        DEFAULT_WAIT_ACCESSIBILITY_EVENT_MS,
        MIN_WAIT_ACCESSIBILITY_EVENT_MS,
        MAX_WAIT_ACCESSIBILITY_EVENT_MS,
    )

    val maxGoBackAfterApps: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_GO_BACK_AFTER_APPS,
        DEFAULT_GO_BACK_AFTER_APPS,
        MIN_GO_BACK_AFTER_APPS,
        MAX_GO_BACK_AFTER_APPS,
    )

    suspend fun setDelayForNextAppTimeout(value: Int) = dataStore.setValue(
        KEY_DELAY_FOR_NEXT_APP_TIMEOUT,
        clamp(value,
            MIN_DELAY_FOR_NEXT_APP_MS,
            MAX_DELAY_FOR_NEXT_APP_MS)
    )

    suspend fun setMaxWaitAppTimeout(value: Int) {
        val currentMaxWaitAppTimeout = clamp(value,
            MIN_WAIT_APP_PERFORM_CLICK_MS,
            MAX_WAIT_APP_PERFORM_CLICK_MS)

        dataStore.setValue(
            KEY_MAX_WAIT_APP_TIMEOUT,
            currentMaxWaitAppTimeout)

        val currentMaxWaitClearCacheButtonTimeout =
            maxWaitClearCacheButtonTimeout.first()
        val clampedValue = clamp(currentMaxWaitClearCacheButtonTimeout,
            MIN_WAIT_CLEAR_CACHE_BUTTON_MS,
            currentMaxWaitAppTimeout)

        // reset clear cache button value, to fit into max wait app timeout
        if (currentMaxWaitClearCacheButtonTimeout != clampedValue) {
            setMaxWaitClearCacheButtonTimeout(
                maxWaitClearCacheButtonTimeout.first())
        }
    }

    suspend fun setMaxWaitClearCacheButtonTimeout(value: Int) {
        // Collect the latest value from maxWaitAppTimeout
        val currentMaxWaitAppTimeout = maxWaitAppTimeout.first()

        dataStore.setValue(
            KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT,
            clamp(value,
                MIN_WAIT_CLEAR_CACHE_BUTTON_MS,
                currentMaxWaitAppTimeout)
        )
    }

    suspend fun setMaxWaitAccessibilityEventTimeout(value: Int) = dataStore.setValue(
        KEY_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT,
        clamp(value,
            MIN_WAIT_ACCESSIBILITY_EVENT_MS,
            MAX_WAIT_ACCESSIBILITY_EVENT_MS)
    )

    suspend fun setMaxGoBackAfterApps(value: Int) = dataStore.setValue(
        KEY_MAX_GO_BACK_AFTER_APPS,
        clamp(value,
            MIN_GO_BACK_AFTER_APPS,
            MAX_GO_BACK_AFTER_APPS)
    )
}