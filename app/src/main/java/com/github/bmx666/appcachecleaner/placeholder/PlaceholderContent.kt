package com.github.bmx666.appcachecleaner.placeholder

import android.app.usage.StorageStats
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.bmx666.appcachecleaner.util.getInternalCacheSize
import java.util.Locale

object PlaceholderContent {

    object All {
        internal val items by lazy { mutableListOf<PlaceholderPackage>() }

        suspend fun reset() {
            items.forEach {
                it.ignore = true
                it.visible = true
                it.checked = false
            }
        }

        suspend fun contains(pkgInfo: PackageInfo): Boolean {
            return items.any { it.name == pkgInfo.packageName }
        }

        suspend fun add(pkgInfo: PackageInfo, label: String, locale: Locale,
                    stats: StorageStats?) {
            items.add(
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

        suspend fun updateStats(pkgInfo: PackageInfo, stats: StorageStats?) {
            items.find { it.name == pkgInfo.packageName }?.let {
                it.stats = stats; it.ignore = false
            }
        }

        suspend fun isSameLabelLocale(pkgInfo: PackageInfo, locale: Locale): Boolean {
            items.find { it.name == pkgInfo.packageName }?.let {
                return it.locale == locale
            } ?: return false
        }

        suspend fun isLabelAsPackageName(pkgInfo: PackageInfo): Boolean {
            items.find { it.name == pkgInfo.packageName }?.let {
                return it.label == pkgInfo.packageName
            } ?: return false
        }

        suspend fun updateLabel(pkgInfo: PackageInfo, label: String, locale: Locale) {
            items.find { it.name == pkgInfo.packageName }?.let {
                it.label = label; it.locale = locale; it.ignore = false
            }
        }

        suspend fun check(list: Set<String>) {
            items.forEach {
                it.checked = list.contains(it.name)
            }
        }

        suspend fun checkVisible() {
            items.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = true }
        }

        suspend fun uncheckVisible() {
            items.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = false }
        }

        suspend fun ignore(list: Set<String>) {
            items.forEach {
                if (list.contains(it.name))
                    it.ignore = true
            }
        }

        suspend fun unignore(list: Set<String>) {
            items.forEach {
                if (list.contains(it.name))
                    it.ignore = false
            }
        }

        suspend fun getSorted(): List<PlaceholderPackage> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    getSortedByCacheSize()
                else
                    getSortedByLabel()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private suspend fun getSortedByCacheSize(): List<PlaceholderPackage> {
            return items.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenByDescending { it.stats?.cacheBytes ?: 0 }
                    .thenBy { it.label })
        }

        suspend fun getSortedByLabel(): List<PlaceholderPackage> {
            return items.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenBy { it.label })
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getFilteredByCacheSize(minCacheBytes: Long): List<PlaceholderPackage> {
            val compareCacheBytes: (cacheBytes: Long?) -> Boolean = { cacheBytes ->
                cacheBytes?.let { v -> v >= minCacheBytes } ?: false
            }

            return items.filterNot { it.ignore }
                .onEach { it.visible = compareCacheBytes(it.stats?.cacheBytes) }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenByDescending { it.stats?.cacheBytes ?: 0 }
                    .thenBy { it.label })
        }

        suspend fun getFilteredByName(text: String): List<PlaceholderPackage> {
            val finalText = text.trim().lowercase()
            val finalTextIsNotEmpty = finalText.isNotEmpty()
            val compareLabel: (label: String) -> Boolean = { label ->
                if (finalTextIsNotEmpty)
                    label.lowercase().contains(finalText)
                else true
            }

            return items.filterNot { it.ignore }
                .onEach { it.visible = compareLabel(it.label) }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenBy { it.label })
        }
    }

    object Current {
        internal var items: List<PlaceholderPackage> = ArrayList()

        suspend fun update(list: List<PlaceholderPackage>) {
            items = list
        }

        suspend fun getVisible(): List<PlaceholderPackage> {
            return items.filter { it.visible }
        }

        suspend fun getChecked(): List<PlaceholderPackage> {
            return items.filter { it.checked }
        }

        suspend fun getPackageNames(): List<String> {
            return items.map { it.name }
        }

        suspend fun getCheckedPackageNames(): List<String> {
            return items.filter { it.checked }.map { it.name }
        }

        suspend fun isAllVisibleChecked(): Boolean {
            return items.filter { it.visible }.all { it.checked }
        }

        suspend fun isAllVisibleUnchecked(): Boolean {
            return items.filter { it.visible }.none { it.checked }
        }

        suspend fun find(pkgName: String): PlaceholderPackage? {
            return items.firstOrNull{ it.name == pkgName }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getCheckedTotalCacheSize(): Long {
            return All.items.filter { it.checked }.sumOf { it.getCacheSize() }
        }
    }

    data class PlaceholderPackage(val pkgInfo: PackageInfo, val name: String,
                                  var label: String, var locale: Locale,
                                  var stats: StorageStats?, var visible: Boolean,
                                  var checked: Boolean, var ignore: Boolean) {
        override fun toString(): String = name

        @RequiresApi(Build.VERSION_CODES.O)
        fun getCacheSize(): Long = stats?.getInternalCacheSize() ?: 0L
    }
}