package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefFirstBootManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirstBootViewModel @Inject constructor(
    private val userPrefFirstBootManager: UserPrefFirstBootManager,
) : ViewModel() {

    val firstBoot: StateFlow<Boolean?> =
        userPrefFirstBootManager.firstBoot.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        firstBoot as StateFlow<Any?>,
    )

    fun setFirstBootCompleted() {
        viewModelScope.launch {
            userPrefFirstBootManager.setFirstBootCompleted()
        }
    }
}