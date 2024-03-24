package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefFilterManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsFilterViewModel @Inject constructor(
    private val userPrefFilterManager: UserPrefFilterManager,
) : ViewModel() {

    val minCacheSizeBytes: StateFlow<Long?> =
        userPrefFilterManager.minCacheSizeBytes.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val hideDisabledApps: StateFlow<Boolean?> =
        userPrefFilterManager.hideDisabledApps.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val hideIgnoredApps: StateFlow<Boolean?> =
        userPrefFilterManager.hideIgnoredApps.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val showDialogToIgnoreApp: StateFlow<Boolean?> =
        userPrefFilterManager.showDialogToIgnoreApp.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val listOfIgnoredApps: StateFlow<Set<String>?> =
        userPrefFilterManager.listOfIgnoredApps.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        minCacheSizeBytes as StateFlow<Any?>,
        hideIgnoredApps as StateFlow<Any?>,
        hideDisabledApps as StateFlow<Any?>,
        showDialogToIgnoreApp as StateFlow<Any?>,
        listOfIgnoredApps as StateFlow<Any?>,
    )

    fun setMinCacheSizeBytes(value: Long) {
        viewModelScope.launch {
            userPrefFilterManager.setMinCacheSizeBytes(value)
        }
    }

    fun removeMinCacheSizeBytes() {
        viewModelScope.launch {
            userPrefFilterManager.removeMinCacheSizeBytes()
        }
    }

    fun toggleHideDisabledApps() {
        viewModelScope.launch {
            userPrefFilterManager.toggleHideDisabledApps()
        }
    }

    fun toggleHideIgnoredApps() {
        viewModelScope.launch {
            userPrefFilterManager.toggleHideIgnoredApps()
        }
    }

    fun toggleShowDialogToIgnoreApp() {
        viewModelScope.launch {
            userPrefFilterManager.toggleShowDialogToIgnoreApp()
        }
    }

    fun setListOfIgnoredApps(value: Set<String>) {
        viewModelScope.launch {
            userPrefFilterManager.setListOfIgnoredApps(value)
        }
    }
}