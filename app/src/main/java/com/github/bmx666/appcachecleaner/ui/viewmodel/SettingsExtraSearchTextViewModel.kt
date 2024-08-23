package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefExtraSearchTextManager
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import com.github.bmx666.appcachecleaner.util.combineNull
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsExtraSearchTextViewModel @Inject constructor(
    private val userPrefExtraSearchTextManager: UserPrefExtraSearchTextManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val clearCache: StateFlow<String?> = getClearCache(
        runBlocking { LocaleHelper.getCurrentLocale(context) }
    )

    val storage: StateFlow<String?> = getStorage(
        runBlocking { LocaleHelper.getCurrentLocale(context) }
    )

    val forceStop: StateFlow<String?> = getForceStop(
        runBlocking { LocaleHelper.getCurrentLocale(context) }
    )

    val isReady: StateFlow<Boolean> = combineNull(
        viewModelScope,
        clearCache as StateFlow<Any?>,
        storage as StateFlow<Any?>,
        forceStop as StateFlow<Any?>,
    )

    fun getClearCache(locale: Locale): StateFlow<String?> {
        return userPrefExtraSearchTextManager.getClearCache(locale).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )
    }

    fun setClearCache(locale: Locale, value: CharSequence?) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.setClearCache(locale, value)
        }
    }

    fun removeClearCache(locale: Locale) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.removeClearCache(locale)
        }
    }

    fun getStorage(locale: Locale): StateFlow<String?> {
        return userPrefExtraSearchTextManager.getStorage(locale).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )
    }

    fun setStorage(locale: Locale, value: CharSequence?) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.setStorage(locale, value)
        }
    }

    fun removeStorage(locale: Locale) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.removeStorage(locale)
        }
    }

    fun getForceStop(locale: Locale): StateFlow<String?> {
        return userPrefExtraSearchTextManager.getForceStop(locale).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )
    }

    fun setForceStop(locale: Locale, value: CharSequence?) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.setForceStop(locale, value)
        }
    }

    fun removeForceStop(locale: Locale) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.removeForceStop(locale)
        }
    }
}