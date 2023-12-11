package com.github.bmx666.appcachecleaner.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

suspend fun DataStore<Preferences>.toggle(key: Preferences.Key<Boolean>, defValue: Boolean) {
    this.edit { preferences ->
        val current = preferences[key] ?: defValue
        preferences[key] = !current
    }
}

suspend fun <T> DataStore<Preferences>.removeValue(key: Preferences.Key<T>) {
    this.edit { preferences ->
        preferences.remove(key)
    }
}

suspend fun <T> DataStore<Preferences>.setValue(key: Preferences.Key<T>, value: T) {
    this.edit { preferences ->
        preferences[key] = value
    }
}

suspend fun <T : Enum<T>> DataStore<Preferences>.setEnumValue(
    key: Preferences.Key<String>,
    value: T
) {
    this.edit { preferences ->
        preferences[key] = value.name
    }
}