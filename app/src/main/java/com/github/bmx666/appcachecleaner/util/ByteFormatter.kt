package com.github.bmx666.appcachecleaner.util

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

// Pure, framework-free byte formatting + parsing. Replaces the vendored Spring
// org.springframework.util.unit.DataSize: format() reproduces the old
// DataSize.toFormattedString thresholds, parse() reproduces DataSize.parse semantics
// (integer magnitude + optional unit suffix). java.text (not android.icu) so it is
// unit-testable on a plain JVM without Robolectric.
object ByteFormatter {

    private const val UNIT = 1024.0

    // Human-readable size. Picks the largest unit whose value stays <= 900 (matching the
    // previous behavior), formats with up to 2 fraction digits in the given locale, and
    // appends a fixed ASCII unit (KB/MB/GB/TB) so the suffix never localizes.
    fun format(bytes: Long, locale: Locale): String {
        val nf: NumberFormat = NumberFormat.getInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
            isGroupingUsed = false
            (this as? DecimalFormat)?.roundingMode = RoundingMode.HALF_UP
        }

        val kb = bytes / UNIT
        val mb = kb / UNIT
        val gb = mb / UNIT
        val tb = gb / UNIT

        return when {
            kb <= 900.0 -> "${nf.format(kb)} KB"
            mb <= 900.0 -> "${nf.format(mb)} MB"
            gb <= 900.0 -> "${nf.format(gb)} GB"
            else -> "${nf.format(tb)} TB"
        }
    }

    // Parse "<number><unit>" into bytes; unit optional (defaults to bytes). Whitespace is
    // ignored, suffix is case-insensitive. Returns null on any malformed input (callers
    // use this for both validation and the actual save).
    fun parse(text: String): Long? {
        val cleaned = text.trim().replace(" ", "")
        if (cleaned.isEmpty()) return null

        val match = PATTERN.matchEntire(cleaned) ?: return null
        val magnitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2].uppercase()) {
            "", "B" -> 1.0
            "KB" -> UNIT
            "MB" -> UNIT * UNIT
            "GB" -> UNIT * UNIT * UNIT
            "TB" -> UNIT * UNIT * UNIT * UNIT
            else -> return null
        }
        val bytes = magnitude * multiplier
        if (bytes < 0.0) return null
        return bytes.toLong()
    }

    private val PATTERN = Regex("([+-]?\\d+(?:\\.\\d+)?)([a-zA-Z]{0,2})")
}
