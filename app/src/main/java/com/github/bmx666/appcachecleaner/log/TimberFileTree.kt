package com.github.bmx666.appcachecleaner.log

import timber.log.Timber
import java.io.File


class TimberFileTree(private val log: File) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        log.appendText(message + "\n")
    }
}