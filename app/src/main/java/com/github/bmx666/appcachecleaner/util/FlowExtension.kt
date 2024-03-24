package com.github.bmx666.appcachecleaner.util

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

fun <T> Flow<Preferences>.getValue(key: Preferences.Key<T>, defValue: T): Flow<T> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                Timber.e("Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                Timber.e("Unknown error occurred, emitting default value.", exception)
                emit(emptyPreferences())
            }
        }
        .map { preferences ->
            preferences[key] ?: defValue
        }
}

fun <T> Flow<Preferences>.getValueOrNull(key: Preferences.Key<T>, defValue: T? = null): Flow<T?> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                Timber.e("Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                Timber.e("Unknown error occurred, emitting default value.", exception)
                emit(emptyPreferences())
            }
        }
        .map { preferences ->
            preferences[key] ?: defValue
        }
}

inline fun <reified T : Enum<T>> Flow<Preferences>.getEnumValue(
    key: Preferences.Key<String>,
    defValue: T
): Flow<T> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                Timber.e("Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                Timber.e("Unknown error occurred, emitting default value.", exception)
                emit(emptyPreferences())
            }
        }
        .map { preferences ->
            preferences[key]?.let { stringValue ->
                try {
                    enumValueOf<T>(stringValue)
                } catch (e: IllegalArgumentException) {
                    defValue
                }
            } ?: defValue
        }
}
