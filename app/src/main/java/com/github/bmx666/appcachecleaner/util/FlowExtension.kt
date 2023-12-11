package com.github.bmx666.appcachecleaner.util

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.github.bmx666.appcachecleaner.log.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

fun <T> Flow<Preferences>.getValue(
    key: Preferences.Key<T>,
    defValue: T,
    minValue: T? = null,
    maxValue: T? = null,
): Flow<T> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                Logger.e("Error reading preferences. $exception")
                emit(emptyPreferences())
            } else {
                Logger.e("Unknown error occurred, emitting default value. $exception")
                emit(emptyPreferences())
            }
        }
        .map { preferences ->
            val value = preferences[key] ?: defValue
            when (value) {
                is Int -> clamp(value, minValue as? Int, maxValue as? Int) as T
                else -> value
            }
        }
}

fun <T> Flow<Preferences>.getValueOrNull(
    key: Preferences.Key<T>,
    defValue: T? = null,
    minValue: T? = null,
    maxValue: T? = null,
): Flow<T?> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                Logger.e("Error reading preferences. $exception")
                emit(emptyPreferences())
            } else {
                Logger.e("Unknown error occurred, emitting default value. $exception")
                emit(emptyPreferences())
            }
        }
        .map { preferences ->
            val value = preferences[key] ?: defValue
            when (value) {
                is Int -> clamp(value, minValue as? Int, maxValue as? Int) as T
                else -> value
            }
        }
}

inline fun <reified T : Enum<T>> Flow<Preferences>.getEnumValue(
    key: Preferences.Key<String>,
    defValue: T
): Flow<T> {
    return this
        .catch { exception ->
            if (exception is IOException) {
                Logger.e("Error reading preferences. $exception")
                emit(emptyPreferences())
            } else {
                Logger.e("Unknown error occurred, emitting default value. $exception")
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
