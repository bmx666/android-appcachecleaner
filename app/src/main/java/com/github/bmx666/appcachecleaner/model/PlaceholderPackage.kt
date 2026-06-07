package com.github.bmx666.appcachecleaner.model

import android.app.usage.StorageStats
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.bmx666.appcachecleaner.util.getInternalCacheSize
import java.util.Locale

// Selection ("checked") is NOT a field here: it lives as a Set<String> of package
// names in PackageRepository, decoupled from these heavy package objects so it
// survives re-sort/re-filter without touching the list elements.
data class PlaceholderPackage(
    val pkgInfo: PackageInfo,
    val name: String,
    var label: String,
    var locale: Locale,
    var stats: StorageStats?,
    var visible: Boolean,
    var ignore: Boolean,
) {
    override fun toString(): String = name

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCacheSize(): Long = stats?.getInternalCacheSize() ?: 0L
}
