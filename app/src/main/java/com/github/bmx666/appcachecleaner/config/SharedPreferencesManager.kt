package com.github.bmx666.appcachecleaner.config

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SharedPreferencesManager {

    class ExtraSearchText {

        companion object {

            private const val FILENAME = "ExtraSearchText"
            private const val KEY_CLEAR_CACHE = "clear_cache"
            private const val KEY_STORAGE = "storage"

            @JvmStatic
            private fun getExtraSearchTextSharedPref(context: Context): SharedPreferences {
                return context.getSharedPreferences(FILENAME, AppCompatActivity.MODE_PRIVATE)
            }

            @JvmStatic
            fun getClearCache(context: Context, locale: Locale): String? {
                return getExtraSearchTextSharedPref(context)
                    .getString("$locale,$KEY_CLEAR_CACHE", null)
            }

            @JvmStatic
            fun saveClearCache(context: Context, locale: Locale, value: CharSequence?) {
                if (value.isNullOrEmpty() or value!!.trim().isEmpty()) return

                getExtraSearchTextSharedPref(context)
                    .edit()
                    .putString("$locale,$KEY_CLEAR_CACHE", value.toString())
                    .apply()
            }

            @JvmStatic
            fun removeClearCache(context: Context, locale: Locale) {
                getExtraSearchTextSharedPref(context)
                    .edit()
                    .remove("$locale,$KEY_CLEAR_CACHE")
                    .apply()
            }

            @JvmStatic
            fun getStorage(context: Context, locale: Locale): String? {
                return getExtraSearchTextSharedPref(context)
                    .getString("$locale,$KEY_STORAGE", null)
            }

            @JvmStatic
            fun saveStorage(context: Context, locale: Locale, value: CharSequence?) {
                if (value.isNullOrEmpty() or value!!.trim().isEmpty()) return

                getExtraSearchTextSharedPref(context)
                    .edit()
                    .putString("$locale,$KEY_STORAGE", value.toString())
                    .apply()
            }

            @JvmStatic
            fun removeStorage(context: Context, locale: Locale) {
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
            private const val KEY_CHECKED = "checked"

            @JvmStatic
            private fun getCheckedPackagesListSharedPref(context: Context): SharedPreferences {
                return context.getSharedPreferences(FILENAME, AppCompatActivity.MODE_PRIVATE)
            }

            @JvmStatic
            fun saveChecked(context: Context, checkedPkgList: Set<String>) {
                getCheckedPackagesListSharedPref(context)
                    .edit()
                    .putStringSet(KEY_CHECKED, checkedPkgList)
                    .apply()
            }

            @JvmStatic
            fun getChecked(context: Context): Set<String> {
                return getCheckedPackagesListSharedPref(context)
                    .getStringSet(KEY_CHECKED, HashSet()) ?: HashSet()
            }
        }
    }
}