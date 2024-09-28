package com.github.bmx666.appcachecleaner.util

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.encode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}

fun String.decode(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
}