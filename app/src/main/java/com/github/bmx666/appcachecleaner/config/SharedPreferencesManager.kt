package com.github.bmx666.appcachecleaner.config

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import java.util.Locale

class SharedPreferencesManager {

    class ExtraSearchText {

        companion object {

            private const val FILENAME = "ExtraSearchText"
            private const val KEY_CLEAR_CACHE = "clear_cache"
            private const val KEY_STORAGE = "storage"

            @JvmStatic
            private suspend fun getExtraSearchTextSharedPref(context: Context): SharedPreferences {
                return context.getSharedPreferences(FILENAME, AppCompatActivity.MODE_PRIVATE)
            }

            @JvmStatic
            suspend fun getClearCache(context: Context, locale: Locale): String? {
                return getExtraSearchTextSharedPref(context)
                    .getString("$locale,$KEY_CLEAR_CACHE", null)
            }

            @JvmStatic
            suspend fun saveClearCache(context: Context, locale: Locale, value: CharSequence?) {
                if (value.isNullOrEmpty() or value!!.trim().isEmpty()) return

                getExtraSearchTextSharedPref(context)
                    .edit()
                    .putString("$locale,$KEY_CLEAR_CACHE", value.toString())
                    .apply()
            }

            @JvmStatic
            suspend fun removeClearCache(context: Context, locale: Locale) {
                getExtraSearchTextSharedPref(context)
                    .edit()
                    .remove("$locale,$KEY_CLEAR_CACHE")
                    .apply()
            }

            @JvmStatic
            suspend fun getStorage(context: Context, locale: Locale): String? {
                return getExtraSearchTextSharedPref(context)
                    .getString("$locale,$KEY_STORAGE", null)
            }

            @JvmStatic
            suspend fun saveStorage(context: Context, locale: Locale, value: CharSequence?) {
                if (value.isNullOrEmpty() or value!!.trim().isEmpty()) return

                getExtraSearchTextSharedPref(context)
                    .edit()
                    .putString("$locale,$KEY_STORAGE", value.toString())
                    .apply()
            }

            @JvmStatic
            suspend fun removeStorage(context: Context, locale: Locale) {
                getExtraSearchTextSharedPref(context)
                    .edit()
                    .remove("$locale,$KEY_STORAGE")
                    .apply()
            }
        }
    }

    class PackageList {

        companion object {

            private const val FILENAME = "package-list"
            private const val LIST_NAMES = "list_names"

            @JvmStatic
            private suspend fun getCheckedPackagesListSharedPref(context: Context): SharedPreferences {
                return context.getSharedPreferences(FILENAME, AppCompatActivity.MODE_PRIVATE)
            }

            suspend fun getNames(context: Context): Set<String> {
                return getCheckedPackagesListSharedPref(context)
                    .getStringSet(LIST_NAMES, HashSet()) ?: HashSet()
            }

            @JvmStatic
            suspend fun get(context: Context, name: String): Set<String> {
                return getCheckedPackagesListSharedPref(context)
                    .getStringSet(name, HashSet()) ?: HashSet()
            }

            @JvmStatic
            suspend fun save(context: Context, name: String, checkedPkgList: Set<String>) {
                val names = getNames(context) as MutableSet<String>
                names.add(name)
                getCheckedPackagesListSharedPref(context)
                    .edit()
                    .putStringSet(LIST_NAMES, names)
                    .putStringSet(name, checkedPkgList)
                    .apply()
            }

            @JvmStatic
            suspend fun remove(context: Context, name: String) {
                val names = getNames(context) as MutableSet<String>
                names.remove(name)
                getCheckedPackagesListSharedPref(context)
                    .edit()
                    .putStringSet(LIST_NAMES, names)
                    .remove(name)
                    .apply()
            }
        }
    }

    class Extra {

        companion object {
            @JvmStatic
            private suspend fun getDefaultSharedPref(context: Context): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(context)
            }

            @JvmStatic
            suspend fun getShowStartStopService(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(
                        context.getString(R.string.prefs_key_show_button_start_stop_service),
                        false)
            }

            @JvmStatic
            suspend fun getShowCloseApp(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(
                        context.getString(R.string.prefs_key_show_button_close_app),
                        false)
            }

            @JvmStatic
            suspend fun getAfterClearingCacheStopService(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(
                        context.getString(R.string.prefs_key_extra_action_stop_service),
                        false)
            }

            @JvmStatic
            suspend fun getAfterClearingCacheCloseApp(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(
                        context.getString(R.string.prefs_key_extra_action_close_app),
                        false)
            }
        }
    }

    class Filter {

        companion object {

            private const val KEY_MIN_CACHE_SIZE_BYTES = "filter_min_cache_size_bytes"

            @JvmStatic
            private suspend fun getDefaultSharedPref(context: Context): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(context)
            }

            @JvmStatic
            @RequiresApi(Build.VERSION_CODES.O)
            suspend fun getMinCacheSize(context: Context): Long {
                return getDefaultSharedPref(context)
                    .getLong(KEY_MIN_CACHE_SIZE_BYTES, 0L)
            }

            @JvmStatic
            @RequiresApi(Build.VERSION_CODES.O)
            suspend fun saveMinCacheSize(context: Context, value: Long) {
                getDefaultSharedPref(context)
                    .edit()
                    .putLong(KEY_MIN_CACHE_SIZE_BYTES, value)
                    .apply()
            }

            @JvmStatic
            @RequiresApi(Build.VERSION_CODES.O)
            suspend fun removeMinCacheSize(context: Context) {
                getDefaultSharedPref(context)
                    .edit()
                    .remove(KEY_MIN_CACHE_SIZE_BYTES)
                    .apply()
            }

            @JvmStatic
            suspend fun getHideDisabledApps(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(
                        context.getString(R.string.prefs_key_filter_hide_disabled_apps),
                        false)
            }

            @JvmStatic
            suspend fun getHideIgnoredApps(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(
                        context.getString(R.string.prefs_key_filter_hide_ignored_apps),
                        true)
            }

            @JvmStatic
            suspend fun getShowDialogToIgnoreApp(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(
                        context.getString(R.string.prefs_key_filter_show_dialog_to_ignore_app),
                        true)
            }

            @JvmStatic
            suspend fun getListOfIgnoredApps(context: Context): Set<String> {
                val key = context.getString(R.string.prefs_key_filter_list_of_ignored_apps)
                return getDefaultSharedPref(context)
                    .getStringSet(key, HashSet()) ?: HashSet()
            }

            @JvmStatic
            suspend fun setListOfIgnoredApps(context: Context, pkgList: Set<String>) {
                val key = context.getString(R.string.prefs_key_filter_list_of_ignored_apps)
                getDefaultSharedPref(context)
                    .edit()
                    .putStringSet(key, pkgList)
                    .apply()
            }
        }
    }

    class FirstBoot {

        companion object {

            private const val FILENAME = "FirstBoot"
            private const val KEY_SHOW_FIRST_BOOT_CONFIRMATION =
                "show_first_boot_confirmation"

            @JvmStatic
            private suspend fun getDefaultSharedPref(context: Context): SharedPreferences {
                return context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE)
            }

            @JvmStatic
            suspend fun showFirstBootConfirmation(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(KEY_SHOW_FIRST_BOOT_CONFIRMATION, true)
            }

            @JvmStatic
            suspend fun hideFirstBootConfirmation(context: Context) {
                getDefaultSharedPref(context)
                    .edit()
                    .putBoolean(KEY_SHOW_FIRST_BOOT_CONFIRMATION, false)
                    .apply()
            }
        }
    }

    class BugWarning {

        companion object {
            private const val KEY_BUG_322519674 =
                "bug_322519674"

            @JvmStatic
            private suspend fun getDefaultSharedPref(context: Context): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(context)
            }

            @JvmStatic
            suspend fun showBug322519674(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(KEY_BUG_322519674, true)
            }

            @JvmStatic
            suspend fun hideBug322519674(context: Context) {
                getDefaultSharedPref(context)
                    .edit()
                    .putBoolean(KEY_BUG_322519674, false)
                    .apply()
            }
        }
    }

    class UI {
        companion object {
            @JvmStatic
            private suspend fun getDefaultSharedPref(context: Context): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(context)
            }

            @JvmStatic
            suspend fun getNightMode(context: Context): Boolean {
                return getDefaultSharedPref(context)
                    .getBoolean(context.getString(R.string.prefs_key_ui_night_mode), false)
            }

            @JvmStatic
            fun setNightMode(context: Context, value: Boolean) {
                getDefaultSharedPref(context)
                    .edit()
                    .putBoolean(context.getString(R.string.prefs_key_ui_night_mode), value)
                    .apply()
            }
        }
    }

    class Settings {
        companion object {
            @JvmStatic
            private suspend fun getDefaultSharedPref(context: Context): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(context)
            }

            @JvmStatic
            suspend fun getDelayForNextAppTimeout(context: Context): Int {
                return getDefaultSharedPref(context)
                    .getInt(context.getString(R.string.prefs_key_settings_delay_for_next_app_timeout),
                        Constant.Settings.CacheClean.DEFAULT_DELAY_FOR_NEXT_APP_MS / 1000)
            }

            @JvmStatic
            suspend fun setDelayForNextAppTimeout(context: Context, timeout: Int) {
                getDefaultSharedPref(context)
                    .edit()
                    .putInt(context.getString(R.string.prefs_key_settings_delay_for_next_app_timeout), timeout)
                    .apply()
            }

            @JvmStatic
            suspend fun getMaxWaitAppTimeout(context: Context): Int {
                return getDefaultSharedPref(context)
                    .getInt(context.getString(R.string.prefs_key_settings_max_wait_app_timeout),
                        Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS / 1000)
            }

            @JvmStatic
            suspend fun setMaxWaitAppTimeout(context: Context, timeout: Int) {
                getDefaultSharedPref(context)
                    .edit()
                    .putInt(context.getString(R.string.prefs_key_settings_max_wait_app_timeout), timeout)
                    .apply()
            }

            @JvmStatic
            suspend fun getMaxWaitClearCacheButtonTimeout(context: Context): Int {
                return getDefaultSharedPref(context)
                    .getInt(context.getString(R.string.prefs_key_settings_max_wait_clear_cache_btn_timeout),
                        Constant.Settings.CacheClean.DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS / 1000)
            }

            @JvmStatic
            suspend fun setMaxWaitClearCacheButtonTimeout(context: Context, timeout: Int) {
                getDefaultSharedPref(context)
                    .edit()
                    .putInt(context.getString(R.string.prefs_key_settings_max_wait_clear_cache_btn_timeout), timeout)
                    .apply()
            }

            @JvmStatic
            suspend fun getScenario(context: Context): Constant.Scenario {
                try {
                    val value = getDefaultSharedPref(context)
                        .getString(context.getString(R.string.prefs_key_settings_scenario),
                            null) ?: Constant.Scenario.DEFAULT.ordinal.toString()
                    return Constant.Scenario.values()[value.toInt()]
                } catch (_: Exception) {}
                return Constant.Scenario.DEFAULT
            }
        }
    }
}