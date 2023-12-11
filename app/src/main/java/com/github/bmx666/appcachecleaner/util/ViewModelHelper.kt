package com.github.bmx666.appcachecleaner.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

fun combineNonNull(scope: CoroutineScope, vararg flows: StateFlow<Any?>): StateFlow<Boolean> {
    return combine(*flows) { values ->
        values.all { it != null }
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        false
    )
}

fun combineNull(scope: CoroutineScope, vararg flows: StateFlow<Any?>): StateFlow<Boolean> {
    return combine(*flows) { _ ->
        true
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        false
    )
}