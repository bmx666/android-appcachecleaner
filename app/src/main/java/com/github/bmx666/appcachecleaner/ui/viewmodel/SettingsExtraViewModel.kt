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
        showButtonStartStopService as StateFlow<Any?>,
        showButtonCloseApp as StateFlow<Any?>,
        actionStopService as StateFlow<Any?>,
        actionCloseApp as StateFlow<Any?>,
    )

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