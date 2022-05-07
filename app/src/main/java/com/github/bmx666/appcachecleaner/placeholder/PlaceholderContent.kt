package com.github.bmx666.appcachecleaner.placeholder

import android.content.pm.PackageInfo
import java.util.ArrayList

object PlaceholderContent {

    val ITEMS: MutableList<PlaceholderPackage> = ArrayList()

    fun sort() {
        ITEMS.sortWith(compareBy<PlaceholderPackage> { !it.checked }.thenBy { it.label })
    }

    fun addItem(pkgInfo: PackageInfo, label: String, checked: Boolean) {
        ITEMS.add(PlaceholderPackage(pkgInfo, pkgInfo.packageName, label, checked))
    }

    data class PlaceholderPackage(val pkgInfo: PackageInfo, val name: String,
                                  val label: String, var checked: Boolean) {
        override fun toString(): String = name
    }
}