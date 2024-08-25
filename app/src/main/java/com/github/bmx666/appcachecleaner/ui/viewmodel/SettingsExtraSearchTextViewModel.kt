package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefExtraSearchTextManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsExtraSearchTextViewModel @Inject constructor(
    private val userPrefExtraSearchTextManager: UserPrefExtraSearchTextManager,
) : ViewModel() {

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

    fun getClearData(locale: Locale): StateFlow<String?> {
        return userPrefExtraSearchTextManager.getClearData(locale).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )
    }

    fun setClearData(locale: Locale, value: CharSequence?) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.setClearData(locale, value)
        }
    }

    fun removeClearData(locale: Locale) {
        viewModelScope.launch {
            userPrefExtraSearchTextManager.removeClearData(locale)
        }
    }
}