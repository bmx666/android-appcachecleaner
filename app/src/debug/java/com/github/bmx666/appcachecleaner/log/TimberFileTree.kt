package com.github.bmx666.appcachecleaner.log

import android.util.Log
import timber.log.Timber
import java.io.File


class TimberFileTree(private val log: File) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.VERBOSE -> log.appendText("V: $message\n")
            Log.DEBUG   -> log.appendText("D: $message\n")
            Log.INFO    -> log.appendText("I: $message\n")
            Log.WARN    -> log.appendText("W: $message\n")
            Log.ERROR   -> log.appendText("E: $message\n")
            Log.ASSERT  -> log.appendText("A: $message\n")
        }
    }
}