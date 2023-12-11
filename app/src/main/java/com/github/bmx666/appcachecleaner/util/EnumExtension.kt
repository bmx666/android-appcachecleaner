package com.github.bmx666.appcachecleaner.util

inline fun <reified T : Enum<T>> String.getEnumValueOrNull(): T? {
    return try {
        enumValueOf<T>(this)
    } catch (e: IllegalArgumentException) {
        null
    }
}