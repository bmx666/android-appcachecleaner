package com.github.bmx666.appcachecleaner.data

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
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
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_GO_BACK_AFTER_APPS_FOR_API_34
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_ACCESSIBILITY_EVENT_MS
import com.github.bmx666.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_APP_PERFORM_CLICK_MS
import com.github.bmx666.appcachecleaner.util.clamp
import com.github.bmx666.appcachecleaner.util.getValue
import com.github.bmx666.appcachecleaner.util.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefTimeoutManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "prefsSettings",
            produceMigrations = { context ->
                listOf(
                    SharedPreferencesMigration(
                        context,
                        "shared_prefs_name"
                    )
                )
            }
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
    }

    private val dataStore = context.dataStore

    val delayForNextAppTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_DELAY_FOR_NEXT_APP_TIMEOUT,
        DEFAULT_DELAY_FOR_NEXT_APP_MS
    )

    val maxWaitAppTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_APP_TIMEOUT,
        DEFAULT_WAIT_APP_PERFORM_CLICK_MS
    )

    val maxWaitClearCacheButtonTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT,
        DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS
    )

    val maxWaitAccessibilityEventTimeout: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT,
        DEFAULT_WAIT_ACCESSIBILITY_EVENT_MS
    )

    val maxGoBackAfterApps: Flow<Int> = dataStore.data.getValue(
        KEY_MAX_GO_BACK_AFTER_APPS,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            DEFAULT_GO_BACK_AFTER_APPS
        else
            MIN_GO_BACK_AFTER_APPS
    )

    suspend fun setDelayForNextAppTimeout(value: Int) = dataStore.setValue(
        KEY_DELAY_FOR_NEXT_APP_TIMEOUT,
        clamp(value,
            MIN_DELAY_FOR_NEXT_APP_MS,
            MAX_DELAY_FOR_NEXT_APP_MS)
    )

    suspend fun setMaxWaitAppTimeout(value: Int) = dataStore.setValue(
        KEY_MAX_WAIT_APP_TIMEOUT,
        clamp(value,
            MIN_WAIT_APP_PERFORM_CLICK_MS,
            MAX_WAIT_APP_PERFORM_CLICK_MS)
    )

    suspend fun setMaxWaitClearCacheButtonTimeout(value: Int) = dataStore.setValue(
        KEY_MAX_WAIT_CLEAR_CACHE_BTN_TIMEOUT, value
    )

    suspend fun setMaxWaitAccessibilityEventTimeout(value: Int) = dataStore.setValue(
        KEY_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT,
        clamp(value,
            MIN_WAIT_ACCESSIBILITY_EVENT_MS,
            MAX_WAIT_ACCESSIBILITY_EVENT_MS)
    )

    suspend fun setMaxGoBackAfterApps(value: Int) = dataStore.setValue(
        KEY_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT,
        clamp(value,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                MIN_GO_BACK_AFTER_APPS_FOR_API_34
            else
                MIN_GO_BACK_AFTER_APPS,
            MAX_GO_BACK_AFTER_APPS)
    )
}