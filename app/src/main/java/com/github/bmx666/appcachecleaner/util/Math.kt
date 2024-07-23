package com.github.bmx666.appcachecleaner.util

fun clamp(value: Int, min: Int, max: Int): Int {
    return value.coerceIn(min, max)
}