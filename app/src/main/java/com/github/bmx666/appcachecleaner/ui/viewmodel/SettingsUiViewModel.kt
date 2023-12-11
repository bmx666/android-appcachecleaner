package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.const.Constant.Settings.UI.Contrast
import com.github.bmx666.appcachecleaner.data.UserPrefUiManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsUiViewModel @Inject constructor(
    private val userPrefUiManager: UserPrefUiManager,
) : ViewModel() {

    val forceNightMode: StateFlow<Boolean?> = userPrefUiManager.forceNightMode.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        null
    )

    val dynamicColor: StateFlow<Boolean?> = userPrefUiManager.dynamicColor.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        null
    )

    val contrast: StateFlow<Contrast?> = userPrefUiManager.contrast.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        null
    )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        forceNightMode as StateFlow<Any?>,
        dynamicColor as StateFlow<Any?>,
        contrast as StateFlow<Any?>,
    )

    fun toggleForceNightMode() {
        viewModelScope.launch {
            userPrefUiManager.toggleForceNightMode()
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            userPrefUiManager.toggleDynamicColor()
        }
    }

    fun setContrast(contrast: Contrast) {
        viewModelScope.launch {
            userPrefUiManager.setContrast(contrast)
        }
    }
}