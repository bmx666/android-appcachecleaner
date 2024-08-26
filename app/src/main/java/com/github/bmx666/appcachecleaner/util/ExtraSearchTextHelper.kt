package com.github.bmx666.appcachecleaner.util

import android.content.Context
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.data.UserPrefExtraSearchTextManager
import com.github.bmx666.appcachecleaner.data.UserPrefScenarioManager
import kotlinx.coroutines.flow.firstOrNull

class ExtraSearchTextHelper {

    companion object {

        @JvmStatic
        private suspend fun getClearCache(context: Context): CharSequence? {
            val locale = LocaleHelper.getCurrentLocale(context)
            val manager = UserPrefExtraSearchTextManager(context.applicationContext)
            return manager.getClearCache(locale).firstOrNull()
        }

        @JvmStatic
        private suspend fun getStorage(context: Context): CharSequence? {
            val locale = LocaleHelper.getCurrentLocale(context)
            val manager = UserPrefExtraSearchTextManager(context.applicationContext)
            return manager.getStorage(locale).firstOrNull()
        }

        @JvmStatic
        private suspend fun getForceStop(context: Context): CharSequence? {
            val locale = LocaleHelper.getCurrentLocale(context)
            val manager = UserPrefExtraSearchTextManager(context.applicationContext)
            return manager.getForceStop(locale).firstOrNull()
        }

        @JvmStatic
        private suspend fun getScenario(context: Context): Constant.Scenario {
            val manager = UserPrefScenarioManager(context.applicationContext)
            return manager.scenario.firstOrNull() ?: Constant.Scenario.DEFAULT
        }

        @JvmStatic
        suspend fun getTextForOk(context: Context): ArrayList<CharSequence> {
            val list = ArrayList<CharSequence>()

            when (getScenario(context)) {
                Constant.Scenario.DEFAULT -> {}
                Constant.Scenario.XIAOMI_MIUI -> {
                    arrayListOf(
                        "app_manager_dlg_ok",
                    ).forEach { resourceName ->
                        PackageManagerHelper.getApplicationResourceString(
                            context,"com.miui.securitycenter", resourceName)?.let { value ->
                            if (value.isNotEmpty())
                                list.add(value)
                        }
                    }
                }
            }

            list.add(context.getText(android.R.string.ok))

            return list
        }

        @JvmStatic
        suspend fun getTextForClearCache(context: Context): ArrayList<CharSequence> {
            val list = ArrayList<CharSequence>()

            getClearCache(context)?.let { value ->
                if (value.isNotEmpty())
                    list.add(value)
            }

            when (getScenario(context)) {
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
                Constant.Scenario.XIAOMI_MIUI -> {
                    arrayListOf(
                        "app_manager_clear_cache",
                    ).forEach { resourceName ->
                        PackageManagerHelper.getApplicationResourceString(
                            context,"com.miui.securitycenter", resourceName)?.let { value ->
                            if (value.isNotEmpty())
                                list.add(value)
                        }
                    }
                }
            }

            list.add(context.getText(R.string.clear_cache_btn_text))

            return list
        }

        @JvmStatic
        suspend fun getTextForClearData(context: Context): ArrayList<CharSequence> {
            val list = ArrayList<CharSequence>()

            when (getScenario(context)) {
                Constant.Scenario.DEFAULT -> {
                    arrayListOf(
                        "clear_user_data_text",
                    ).forEach { resourceName ->
                        PackageManagerHelper.getApplicationResourceString(
                            context,"com.android.settings", resourceName)?.let { value ->
                            if (value.isNotEmpty())
                                list.add(value)
                        }
                    }
                }
                Constant.Scenario.XIAOMI_MIUI -> {
                    arrayListOf(
                        "app_manager_menu_clear_data",
                    ).forEach { resourceName ->
                        PackageManagerHelper.getApplicationResourceString(
                            context,"com.miui.securitycenter", resourceName)?.let { value ->
                            if (value.isNotEmpty())
                                list.add(value)
                        }
                    }
                }
            }

            list.add(context.getText(R.string.clear_user_data_text))

            return list
        }

        @JvmStatic
        suspend fun getTextForStorage(context: Context): ArrayList<CharSequence> {
            val list = ArrayList<CharSequence>()

            getStorage(context)?.let { value ->
                if (value.isNotEmpty())
                    list.add(value)
            }

            when (getScenario(context)) {
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
                Constant.Scenario.XIAOMI_MIUI -> {}
            }

            list.add(context.getText(R.string.storage_settings_for_app))
            list.add(context.getText(R.string.storage_label))

            return list
        }

        @JvmStatic
        suspend fun getTextForForceStop(context: Context): ArrayList<CharSequence> {
            val list = ArrayList<CharSequence>()

            getForceStop(context)?.let { value ->
                if (value.isNotEmpty())
                    list.add(value)
            }

            when (getScenario(context)) {
                Constant.Scenario.DEFAULT -> {}
                Constant.Scenario.XIAOMI_MIUI -> {}
            }

            list.add(context.getText(R.string.force_stop))

            return list
        }

        @JvmStatic
        suspend fun getTextForForceStopDialogTitle(context: Context): ArrayList<CharSequence> {
            val list = ArrayList<CharSequence>()

            when (getScenario(context)) {
                Constant.Scenario.DEFAULT -> {}
                Constant.Scenario.XIAOMI_MIUI -> {}
            }

            list.add(context.getText(R.string.force_stop_dlg_title))

            return list
        }
    }
}