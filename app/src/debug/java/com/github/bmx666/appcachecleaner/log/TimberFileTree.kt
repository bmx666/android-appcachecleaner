package com.github.bmx666.appcachecleaner.log

import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale


class TimberFileTree(private val log: File) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val formattedTime = sdf.format(System.currentTimeMillis())
        when (priority) {
            Log.VERBOSE -> log.appendText("[$formattedTime] V: $message\n")
            Log.DEBUG   -> log.appendText("[$formattedTime] D: $message\n")
            Log.INFO    -> log.appendText("[$formattedTime] I: $message\n")
            Log.WARN    -> log.appendText("[$formattedTime] W: $message\n")
            Log.ERROR   -> log.appendText("[$formattedTime] E: $message\n")
            Log.ASSERT  -> log.appendText("[$formattedTime] A: $message\n")
        }
    }
}