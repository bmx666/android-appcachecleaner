package com.github.bmx666.appcachecleaner.log

import org.jetbrains.annotations.NonNls
import timber.log.Timber
import java.io.File


class Logger {

    companion object {
        @JvmStatic private fun initializeTimber(logFile: File) {
            Timber.plant(TimberFileTree(logFile))
        }

        @JvmStatic fun v(@NonNls message: String) { Timber.v(message) }
        @JvmStatic fun d(@NonNls message: String) { Timber.d(message) }
        @JvmStatic fun i(@NonNls message: String) { Timber.i(message) }
        @JvmStatic fun w(@NonNls message: String) { Timber.w(message) }
        @JvmStatic fun e(@NonNls message: String) { Timber.e(message) }
        @JvmStatic fun wtf(@NonNls message: String) { Timber.wtf(message) }
    }

    private lateinit var cacheDir: File

    fun onCreate(cacheDir: File) {
        this.cacheDir = cacheDir
        cleanLogFile()
        initializeTimber(getLogFile())
    }

    fun onClearCache() {
        cleanLogFile()
    }

    fun onDestroy() {
        deleteLogFile()
    }

    private fun getLogFile(): File {
        return File(cacheDir.absolutePath + "/log.txt")
    }

    private fun cleanLogFile() {
        // force clean previous log
        getLogFile().writeText("")
    }

    private fun deleteLogFile() {
        getLogFile().delete()
    }
}