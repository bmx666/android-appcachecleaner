package com.github.bmx666.appcachecleaner.util

import android.content.Context
import android.os.Build
import java.util.Locale

class LocaleHelper {

    companion object {

        @JvmStatic
        fun getCurrentLocale(context: Context): Locale {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.resources.configuration.locales.get(0)
            else
                context.resources.configuration.locale
        }
    }
}