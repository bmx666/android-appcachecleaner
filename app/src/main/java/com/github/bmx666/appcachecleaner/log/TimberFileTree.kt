package com.github.bmx666.appcachecleaner.log

import android.util.Log
import timber.log.Timber
import java.io.File


class TimberFileTree(private val log: File) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.VERBOSE -> log.appendText("V: ")
            Log.DEBUG   -> log.appendText("D: ")
            Log.INFO    -> log.appendText("I: ")
            Log.WARN    -> log.appendText("W: ")
            Log.ERROR   -> log.appendText("E: ")
            Log.ASSERT  -> log.appendText("A: ")
        }
        log.appendText(message + "\n")
    }
}