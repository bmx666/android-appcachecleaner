package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.util.PermissionChecker
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
): ViewModel()
{
    private val _hasAccessibilityPermission = MutableStateFlow(false)
    val hasAccessibilityPermission: StateFlow<Boolean?> =
        _hasAccessibilityPermission.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    private val _hasUsageStatsPermission = MutableStateFlow(false)
    val hasUsageStatsPermission: StateFlow<Boolean?> =
        _hasUsageStatsPermission.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        hasAccessibilityPermission as StateFlow<Any?>,
        hasUsageStatsPermission as StateFlow<Any?>,
    )

    init {
        checkUsageStatsPermission()
        checkAccessibilityPermission()
    }

    fun checkUsageStatsPermission() {
        viewModelScope.launch {
            _hasUsageStatsPermission.value =
                PermissionChecker.checkUsageStatsPermission(context)
        }
    }

    fun checkAccessibilityPermission() {
        viewModelScope.launch {
            _hasAccessibilityPermission.value =
                PermissionChecker.checkAccessibilityPermission(context)
        }
    }
}