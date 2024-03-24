package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefTimeoutManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsTimeoutViewModel @Inject constructor(
    private val userPrefTimeoutManager: UserPrefTimeoutManager,
) : ViewModel() {

    val delayForNextAppTimeout: StateFlow<Int?> =
        userPrefTimeoutManager.delayForNextAppTimeout.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val maxWaitAppTimeout: StateFlow<Int?> =
        userPrefTimeoutManager.maxWaitAppTimeout.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val maxWaitClearCacheButtonTimeout: StateFlow<Int?> =
        userPrefTimeoutManager.maxWaitClearCacheButtonTimeout.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        delayForNextAppTimeout as StateFlow<Any?>,
        maxWaitAppTimeout as StateFlow<Any?>,
        maxWaitClearCacheButtonTimeout as StateFlow<Any?>,
    )

    fun setDelayForNextAppTimeout(value: Int) {
        viewModelScope.launch {
            userPrefTimeoutManager.setDelayForNextAppTimeout(value)
        }
    }

    fun setMaxWaitAppTimeout(value: Int) {
        viewModelScope.launch {
            userPrefTimeoutManager.setMaxWaitAppTimeout(value)
        }
    }

    fun setMaxWaitClearCacheButtonTimeout(value: Int) {
        viewModelScope.launch {
            userPrefTimeoutManager.setMaxWaitClearCacheButtonTimeout(value)
        }
    }
}