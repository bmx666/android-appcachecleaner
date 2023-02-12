package com.github.bmx666.appcachecleaner.placeholder

import android.app.usage.StorageStats
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

object PlaceholderContent {

    private val allItems: MutableList<PlaceholderPackage> = ArrayList()
    private var currentItems: List<PlaceholderPackage> = ArrayList()

    fun resetAll() {
        allItems.forEach {
            it.ignore = true
            it.visible = true
            it.checked = false
        }
    }

    fun getItems(): List<PlaceholderPackage> {
        return currentItems
    }

    fun getVisibleItems(): List<PlaceholderPackage> {
        return currentItems.filter { it.visible }
    }

    fun contains(pkgInfo: PackageInfo): Boolean {
        return allItems.any { it.name == pkgInfo.packageName }
    }

    fun check(checkedList: Set<String>) {
        allItems.forEach {
            it.checked = checkedList.contains(it.name)
        }
    }

    fun sort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            sortByCacheSize()
        else
            sortByLabel()
    }

    fun sortByLabel() {
        currentItems =
            allItems.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenBy { it.label })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sortByCacheSize() {
        currentItems =
            allItems.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenByDescending { it.stats?.cacheBytes ?: 0 }
                    .thenBy { it.label })
    }

    fun filterByName(text: String) {
        val finalText = text.trim().lowercase()
        val finalTextIsNotEmpty = finalText.isNotEmpty()
        val compareLabel: (label: String) -> Boolean = { label ->
            if (finalTextIsNotEmpty)
                label.lowercase().contains(finalText)
            else true
        }

        currentItems =
            allItems.filterNot { it.ignore }
                .onEach { it.visible = compareLabel(it.label) }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenBy { it.label })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun filterByCacheSize(minCacheBytes: Long) {
        val compareCacheBytes: (cacheBytes: Long?) -> Boolean = { cacheBytes ->
            cacheBytes?.let { v -> v >= minCacheBytes } ?: false
        }

        currentItems =
            allItems.filterNot { it.ignore }
                .onEach { it.visible = compareCacheBytes(it.stats?.cacheBytes) }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenByDescending { it.stats?.cacheBytes ?: 0 }
                    .thenBy { it.label })
    }

    fun addItem(pkgInfo: PackageInfo, label: String, locale: Locale,
                stats: StorageStats?) {
        allItems.add(
            PlaceholderPackage(
                pkgInfo = pkgInfo,
                name = pkgInfo.packageName,
                label = label,
                locale = locale,
                stats = stats,
                visible = true,
                checked = false,
                ignore = false))
    }

    fun updateStats(pkgInfo: PackageInfo, stats: StorageStats?) {
        allItems.find { it.name == pkgInfo.packageName }?.let {
            it.stats = stats; it.ignore = false
        }
    }

    fun isSameLabelLocale(pkgInfo: PackageInfo, locale: Locale): Boolean {
        allItems.find { it.name == pkgInfo.packageName }?.let {
            return it.locale == locale
        } ?: return false
    }

    fun updateLabel(pkgInfo: PackageInfo, label: String, locale: Locale) {
        allItems.find { it.name == pkgInfo.packageName }?.let {
            it.label = label; it.locale = locale; it.ignore = false
        }
    }

    fun getAllChecked(): List<String> {
        return currentItems.filter { it.checked }.map { it.name }
    }

    fun isAllVisibleChecked(): Boolean {
        return currentItems.filter { it.visible }.all { it.checked }
    }

    fun isAllVisibleUnchecked(): Boolean {
        return currentItems.filter { it.visible }.none { it.checked }
    }

    fun checkAllVisible() {
        allItems.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = true }
    }

    fun uncheckAllVisible() {
        allItems.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = false }
    }

    data class PlaceholderPackage(val pkgInfo: PackageInfo, val name: String,
                                  var label: String, var locale: Locale,
                                  var stats: StorageStats?, var visible: Boolean,
                                  var checked: Boolean, var ignore: Boolean) {
        override fun toString(): String = name
    }
}