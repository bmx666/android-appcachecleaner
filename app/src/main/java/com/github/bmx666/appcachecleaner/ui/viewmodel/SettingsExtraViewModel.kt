package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefExtraManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsExtraViewModel @Inject constructor(
    private val userPrefExtraManager: UserPrefExtraManager,
) : ViewModel() {

    val showButtonCleanCacheDisabledApps: StateFlow<Boolean?> =
        userPrefExtraManager.showButtonCleanCacheDisabledApps.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val showButtonStartStopService: StateFlow<Boolean?> =
        userPrefExtraManager.showButtonStartStopService.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val showButtonCloseApp: StateFlow<Boolean?> =
        userPrefExtraManager.showButtonCloseApp.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val actionForceStopApps: StateFlow<Boolean?> =
        userPrefExtraManager.actionForceStopApps.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val actionStopService: StateFlow<Boolean?> =
        userPrefExtraManager.actionStopService.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val actionCloseApp: StateFlow<Boolean?> =
        userPrefExtraManager.actionCloseApp.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        showButtonCleanCacheDisabledApps as StateFlow<Any?>,
        showButtonStartStopService as StateFlow<Any?>,
        showButtonCloseApp as StateFlow<Any?>,
        actionForceStopApps as StateFlow<Any?>,
        actionStopService as StateFlow<Any?>,
        actionCloseApp as StateFlow<Any?>,
    )

    fun toggleShowButtonCleanCacheDisabledApps() {
        viewModelScope.launch {
            userPrefExtraManager.toggleShowCleanCacheDisabledApps()
        }
    }

    fun toggleShowButtonStartStopService() {
        viewModelScope.launch {
            userPrefExtraManager.toggleShowButtonStartStopService()
        }
    }

    fun toggleShowButtonCloseApp() {
        viewModelScope.launch {
            userPrefExtraManager.toggleShowButtonCloseApp()
        }
    }

    fun toggleActionForceStopApps() {
        viewModelScope.launch {
            userPrefExtraManager.toggleActionForceStopApps()
        }
    }

    fun toggleActionStopService() {
        viewModelScope.launch {
            userPrefExtraManager.toggleActionStopService()
        }
    }

    fun toggleActionCloseApp() {
        viewModelScope.launch {
            userPrefExtraManager.toggleActionCloseApp()
        }
    }
}