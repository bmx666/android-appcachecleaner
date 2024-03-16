package com.github.bmx666.appcachecleaner.util

import android.app.usage.StorageStats
import android.os.Build
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.O)
fun StorageStats.getInternalCacheSize(): Long {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        return cacheBytes

    val cacheSize = cacheBytes - externalCacheBytes
    return if (cacheSize < 0L) 0L
        else cacheSize
}