package com.github.bmx666.appcachecleaner.util

import android.content.Context
import android.content.res.Configuration
import com.github.bmx666.appcachecleaner.data.UserPrefUiManager
import kotlinx.coroutines.flow.first

suspend fun Context.getDayNightModeContext(): Context {
    val userPrefUiManager = UserPrefUiManager(this.applicationContext)
    return when (userPrefUiManager.forceNightMode.first()) {
        true -> {
            val uiModeFlag = Configuration.UI_MODE_NIGHT_YES
            val config = Configuration(this.resources.configuration)
            config.uiMode = uiModeFlag or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
            this.createConfigurationContext(config)
        }
        else -> this
    }
}