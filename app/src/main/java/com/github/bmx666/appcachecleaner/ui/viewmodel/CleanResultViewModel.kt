package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.toFormattedString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.springframework.util.unit.DataSize
import javax.inject.Inject


@HiltViewModel
class CleanResultViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
): ViewModel()
{
    private val _isInterruptedBySystem = MutableStateFlow(false)
    val isInterruptedBySystem: StateFlow<Boolean> = _isInterruptedBySystem

    private val _actions = MutableStateFlow(false)
    val actions: StateFlow<Boolean> = _actions

    var titleText by mutableStateOf("")

    fun resetTitle() {
        titleText = ""
    }

    fun finishClearCache(context: Context, interrupted: Boolean) {
        val resId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (interrupted) {
                    true -> R.string.text_clean_cache_interrupt_processing
                    else -> R.string.text_clean_cache_finish_processing
                }
            } else {
                when (interrupted) {
                    true -> R.string.text_clean_cache_interrupt
                    else -> R.string.text_clean_cache_finish
                }
            }

        titleText = context.getString(resId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            calculateCleanedCache(context, interrupted)

        if (!interrupted)
            _actions.value = true
    }

    fun resetActions() {
        _actions.value = false
    }

    fun showInterruptedBySystemDialog() {
        _isInterruptedBySystem.value = true
    }

    fun resetInterruptedBySystemDialog() {
        _isInterruptedBySystem.value = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateCleanedCache(context: Context, interrupted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanedCacheBytes =
                    PlaceholderContent.Current.getChecked().sumOf {
                        PackageManagerHelper.getCacheSizeDiff(
                            it.stats,
                            PackageManagerHelper.getStorageStats(context, it.pkgInfo)
                        )
                    }
                val sizeStr = DataSize.ofBytes(cleanedCacheBytes)
                    .toFormattedString(context)

                val resId = when (interrupted) {
                    true -> R.string.text_clean_cache_interrupt_display_size
                    else -> R.string.text_clean_cache_finish_display_size
                }

                titleText = context.getString(resId, sizeStr)
            } finally {
            }
        }
    }
}