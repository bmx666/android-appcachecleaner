package com.github.bmx666.appcachecleaner.log

import android.util.Log
import org.jetbrains.annotations.NonNls
import java.io.File


class Logger() {

    companion object {
        private const val TAG: String = "CacheCleanerLog"

        @JvmStatic fun v(@NonNls message: String) { Log.v(TAG, message) }
        @JvmStatic fun d(@NonNls message: String) { Log.d(TAG, message) }
        @JvmStatic fun i(@NonNls message: String) { Log.i(TAG, message) }
        @JvmStatic fun w(@NonNls message: String) { Log.w(TAG, message) }
        @JvmStatic fun e(@NonNls message: String) { Log.e(TAG, message) }
        @JvmStatic fun wtf(@NonNls message: String) { Log.wtf(TAG, message) }
    }

    fun onCreate(cacheDir: File) {}
    fun onClearCache() {}
    fun onDestroy() {}
}