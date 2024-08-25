package com.github.bmx666.appcachecleaner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.LocaleManager
import com.github.bmx666.appcachecleaner.util.combineNonNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocaleViewModel @Inject constructor(
    private val localeManager: LocaleManager,
) : ViewModel() {

    val currentLocale: StateFlow<Locale> get() = localeManager.currentLocale

    val isReady: StateFlow<Boolean> = combineNonNull(
        viewModelScope,
        currentLocale as StateFlow<Any?>
    )
}
