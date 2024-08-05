package com.github.bmx666.appcachecleaner.config

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.github.bmx666.appcachecleaner.R

class SharedPreferencesManager {

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
}