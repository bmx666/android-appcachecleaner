package com.github.bmx666.appcachecleaner.util

import android.content.Context
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant

class ExtraSearchTextHelper {

    companion object {

        @JvmStatic
        fun getTextForClearCache(context: Context): Array<String> {
            val locale = LocaleHelper.getCurrentLocale(context)

            val list = ArrayList<String>()

            SharedPreferencesManager.ExtraSearchText.getClearCache(context, locale)?.let { value ->
                if (value.isNotEmpty())
                    list.add(value)
            }

            when (SharedPreferencesManager.Settings.getScenario(context)) {
                Constant.Scenario.DEFAULT -> {
                    arrayListOf(
                        "clear_cache_btn_text",
                    ).forEach { resourceName ->
                        PackageManagerHelper.getApplicationResourceString(
                            context,"com.android.settings", resourceName)?.let { value ->
                            if (value.isNotEmpty())
                                list.add(value)
                        }
                    }
                }
            }

            return list.toTypedArray()
        }

        @JvmStatic
        fun getTextForStorage(context: Context): Array<String> {
            val locale = LocaleHelper.getCurrentLocale(context)

            val list = ArrayList<String>()

            SharedPreferencesManager.ExtraSearchText.getStorage(context, locale)?.let { value ->
                if (value.isNotEmpty())
                    list.add(value)
            }

            when (SharedPreferencesManager.Settings.getScenario(context)) {
                Constant.Scenario.DEFAULT -> {
                    arrayListOf(
                        "storage_settings_for_app",
                        "storage_label",
                        // not official
                        "storage_use",
                    ).forEach { resourceName ->
                        PackageManagerHelper.getApplicationResourceString(
                            context,"com.android.settings", resourceName)?.let { value ->
                            if (value.isNotEmpty())
                                list.add(value)
                        }
                    }
                }
            }

            return list.toTypedArray()
        }
    }
}