package com.github.bmx666.appcachecleaner.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

class LocaleManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val _currentLocale = MutableStateFlow(runBlocking { LocaleHelper.getCurrentLocale(context) })
    val currentLocale: StateFlow<Locale> get() = _currentLocale

    init {
        val localeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                CoroutineScope(Dispatchers.Main).launch {
                    _currentLocale.value = LocaleHelper.getCurrentLocale(context)
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val filter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
                context.registerReceiver(localeReceiver, filter)
                awaitCancellation()
            } finally {
                context.unregisterReceiver(localeReceiver)
            }
        }
    }
}