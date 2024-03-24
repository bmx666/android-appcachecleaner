package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefCustomPackageListManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsCustomPackageListViewModel @Inject constructor(
    private val userPrefCustomPackageListManager: UserPrefCustomPackageListManager,
) : ViewModel() {

    val listNames: StateFlow<Set<String>?> =
        userPrefCustomPackageListManager.listNames.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        listNames as StateFlow<Any?>,
    )

    fun getCustomPackageList(name: String): StateFlow<Set<String>?> {
        return userPrefCustomPackageListManager.getCustomList(name).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )
    }

    fun setCustomPackageList(name: String, value: Set<String>) {
        viewModelScope.launch {
            userPrefCustomPackageListManager.setCustomList(name, value)
        }
    }

    fun removeCustomPackageList(name: String) {
        viewModelScope.launch {
            userPrefCustomPackageListManager.removeCustomList(name)
        }
    }
}