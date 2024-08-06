package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.UserPrefFilterManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import com.github.bmx666.appcachecleaner.util.toFormattedString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.springframework.util.unit.DataSize
import javax.inject.Inject

@HiltViewModel
class SettingsFilterViewModel @Inject constructor(
    private val userPrefFilterManager: UserPrefFilterManager,
    @ApplicationContext private val context: Context

) : ViewModel() {

    val minCacheSizeBytes: StateFlow<Long?> =
        userPrefFilterManager.minCacheSizeBytes.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    @RequiresApi(Build.VERSION_CODES.O)
    val minCacheSizeString: StateFlow<String?> =
        minCacheSizeBytes.map { bytes ->
            bytes?.let {
                if (bytes > 0)
                    DataSize.ofBytes(bytes).toFormattedString(context = context)
                else
                    null
            }
        }.stateIn(
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

    fun setListOfIgnoredApps(value: Set<String>) {
        viewModelScope.launch {
            userPrefFilterManager.setListOfIgnoredApps(value)
        }
    }
}