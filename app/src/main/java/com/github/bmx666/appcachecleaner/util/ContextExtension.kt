package com.github.bmx666.appcachecleaner.util

import android.content.Context
import android.content.res.Configuration
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager

fun Context.getDayNightModeContext(): Context {
    return when (SharedPreferencesManager.UI.getNightMode(this)) {
        true -> {
            val uiModeFlag = Configuration.UI_MODE_NIGHT_YES
            val config = Configuration(this.resources.configuration)
            config.uiMode = uiModeFlag or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
            this.createConfigurationContext(config)
        }
        else -> this
    }
}