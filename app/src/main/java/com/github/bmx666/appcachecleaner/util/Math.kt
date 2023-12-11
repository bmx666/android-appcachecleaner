package com.github.bmx666.appcachecleaner.util

import kotlin.math.roundToInt

fun clamp(value: Int, min: Int?, max: Int?): Int {
    return when {
        min != null && max != null -> value.coerceIn(min, max)
        min != null && value < min -> min
        max != null && value > max -> max
        else -> value
    }
}

fun nearest(value: Int, nearest: Int): Int {
    require(nearest > 0) { "The rounding value must be greater than 0." }
    return (value / nearest) * nearest
}

fun nearest(value: Float, nearest: Float): Float {
    require(nearest > 0) { "The rounding value must be greater than 0." }
    return (value / nearest).roundToInt() * nearest
}