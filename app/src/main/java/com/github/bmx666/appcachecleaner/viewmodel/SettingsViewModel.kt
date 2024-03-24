package com.github.bmx666.appcachecleaner.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class SettingsViewModel(private val context: Context) : ViewModel() {

    internal val _isNightModeOn: MutableStateFlow<Boolean> = MutableStateFlow(
        runBlocking { SharedPreferencesManager.UI.getNightMode(context) } )

    fun toggleNightMode() {
        _isNightModeOn.value = _isNightModeOn.value.not()
        runBlocking {
            SharedPreferencesManager.UI.setNightMode(context, _isNightModeOn.value)
        }
    }
}