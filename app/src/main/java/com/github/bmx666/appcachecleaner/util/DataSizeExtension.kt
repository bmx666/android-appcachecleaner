package com.github.bmx666.appcachecleaner.util

import android.content.Context
import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import android.os.Build
import androidx.annotation.RequiresApi
import org.springframework.util.unit.DataSize
import java.math.RoundingMode
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
fun DataSize.toFormattedString(context: Context): String {
    return this.toFormattedString(LocaleHelper.getCurrentLocale(context))
}

@RequiresApi(Build.VERSION_CODES.O)
fun DataSize.toFormattedString(locale: Locale): String {
    val numberFormatter = NumberFormat.getInstance(locale)
    numberFormatter.minimumFractionDigits = 0
    numberFormatter.maximumFractionDigits = 2
    numberFormatter.isGroupingUsed = false
    (numberFormatter as? DecimalFormat)?.roundingMode = RoundingMode.HALF_UP.ordinal

    return if (this.toKilobytes() <= 900L)
        String.format(Locale.US, "%s KB",
            numberFormatter.format(this.toBytes() / 1024f))
    else if (this.toMegabytes() <= 900L)
        String.format(Locale.US, "%s MB",
            numberFormatter.format(this.toKilobytes() / 1024f))
    else if (this.toGigabytes() <= 900L)
        String.format(Locale.US, "%s GB",
            numberFormatter.format(this.toMegabytes() / 1024f))
    else
        String.format(Locale.US, "%s TB",
            numberFormatter.format(this.toGigabytes() / 1024f))
}