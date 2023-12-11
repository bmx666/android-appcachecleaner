package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.data.UserPrefScenarioManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScenarioViewModel @Inject constructor(
    private val userPrefScenarioManager: UserPrefScenarioManager,
) : ViewModel() {

    val scenario: StateFlow<Constant.Scenario?> =
        userPrefScenarioManager.scenario.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        scenario as StateFlow<Any?>,
    )

    fun setScenario(value: Constant.Scenario) {
        viewModelScope.launch {
            userPrefScenarioManager.setScenario(value)
        }
    }
}