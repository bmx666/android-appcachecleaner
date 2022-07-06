package com.github.bmx666.appcachecleaner.placeholder

import android.app.usage.StorageStats
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.ArrayList

object PlaceholderContent {

    val ITEMS: MutableList<PlaceholderPackage> = ArrayList()

    fun sort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            sortByCacheSize()
        else
            sortByLabel()
    }

    private fun sortByLabel() {
        ITEMS.sortWith(compareBy<PlaceholderPackage> { !it.checked }.thenBy { it.label })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sortByCacheSize() {
        ITEMS.sortWith(compareBy<PlaceholderPackage> { !it.checked }
            .thenByDescending { it.stats?.cacheBytes ?: 0 }.thenBy { it.label })
    }

    fun addItem(pkgInfo: PackageInfo, label: String, icon: Drawable?,
                checked: Boolean, stats: StorageStats?) {
        ITEMS.add(PlaceholderPackage(pkgInfo, pkgInfo.packageName, label, icon, checked, stats))
    }

    data class PlaceholderPackage(val pkgInfo: PackageInfo, val name: String,
                                  val label: String, val icon: Drawable?,
                                  var checked: Boolean, val stats: StorageStats?) {
        override fun toString(): String = name
    }
}