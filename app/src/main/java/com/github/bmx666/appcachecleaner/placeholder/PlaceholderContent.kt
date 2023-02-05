package com.github.bmx666.appcachecleaner.placeholder

import android.app.usage.StorageStats
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

object PlaceholderContent {

    private val ITEMS: MutableList<PlaceholderPackage> = ArrayList()

    fun getItems(): MutableList<PlaceholderPackage> {
        return ITEMS
    }

    fun contains(pkgInfo: PackageInfo): Boolean {
        return ITEMS.any { it.name == pkgInfo.packageName }
    }

    fun reset() {
        ITEMS.forEach{ it.ignore = true; it.hideStats = false }
    }

    fun hideStats() {
        ITEMS.forEach{ it.hideStats = true }
    }

    fun sort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            sortByCacheSize()
        else
            sortByLabel()
    }

    fun sortByLabel() {
        ITEMS.sortWith(compareBy<PlaceholderPackage> { it.ignore }
            .thenBy{ !it.checked }
            .thenBy { it.label })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sortByCacheSize() {
        ITEMS.sortWith(compareBy<PlaceholderPackage> { it.ignore }
            .thenBy{ !it.checked }
            .thenByDescending { it.stats?.cacheBytes ?: 0 }
            .thenBy { it.label })
    }

    fun addItem(pkgInfo: PackageInfo, label: String, locale: Locale,
                stats: StorageStats?) {
        ITEMS.add(
            PlaceholderPackage(
                pkgInfo = pkgInfo,
                name = pkgInfo.packageName,
                label = label,
                locale = locale,
                stats = stats,
                hideStats = false,
                checked = false,
                ignore = false))
    }

    fun updateStats(pkgInfo: PackageInfo, stats: StorageStats?) {
        ITEMS.find { it.name == pkgInfo.packageName }?.let {
            it.stats = stats; it.ignore = false
        }
    }

    fun isSameLabelLocale(pkgInfo: PackageInfo, locale: Locale): Boolean {
        ITEMS.find { it.name == pkgInfo.packageName }?.let {
            return it.locale == locale
        }
        return false
    }

    fun updateLabel(pkgInfo: PackageInfo, label: String, locale: Locale) {
        ITEMS.find { it.name == pkgInfo.packageName }?.let {
            it.label = label; it.locale = locale; it.ignore = false
        }
    }

    fun getCheckedPackageList(): List<String> {
        return ITEMS
            .filter { it.checked }
            .map { it.name }
    }

    fun getVisibleCheckedPackageList(): List<String> {
        return ITEMS
            .filter { !it.ignore }
            .filter { it.checked }
            .map { it.name }
    }

    fun getVisibleUncheckedPackageList(): List<String> {
        return ITEMS
            .filter { !it.ignore }
            .filter { !it.checked }
            .map { it.name }
    }

    fun isAllCheckedVisible(): Boolean {
        return ITEMS.all { !it.ignore and it.checked }
    }

    fun isAllUncheckedVisible(): Boolean {
        return ITEMS.none { !it.ignore and it.checked }
    }

    fun checkAllVisible() {
        ITEMS.forEach { if (!it.ignore) it.checked = true }
    }

    fun uncheckAllVisible() {
        ITEMS.forEach { if (!it.ignore) it.checked = false }
    }

    data class PlaceholderPackage(val pkgInfo: PackageInfo, val name: String,
                                  var label: String, var locale: Locale,
                                  var stats: StorageStats?, var hideStats: Boolean,
                                  var checked: Boolean, var ignore: Boolean) {
        override fun toString(): String = name
    }
}