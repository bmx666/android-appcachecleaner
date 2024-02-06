package com.github.bmx666.appcachecleaner.placeholder

import android.app.usage.StorageStats
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Locale

object PlaceholderContent {

    object All {
        internal val items by lazy { mutableListOf<PlaceholderPackage>() }

        fun reset() {
            items.forEach {
                it.ignore = true
                it.visible = true
                it.checked = false
            }
        }

        fun contains(pkgInfo: PackageInfo): Boolean {
            return items.any { it.name == pkgInfo.packageName }
        }

        fun add(pkgInfo: PackageInfo, label: String, locale: Locale,
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

        fun updateStats(pkgInfo: PackageInfo, stats: StorageStats?) {
            items.find { it.name == pkgInfo.packageName }?.let {
                it.stats = stats; it.ignore = false
            }
        }

        fun isSameLabelLocale(pkgInfo: PackageInfo, locale: Locale): Boolean {
            items.find { it.name == pkgInfo.packageName }?.let {
                return it.locale == locale
            } ?: return false
        }

        fun isLabelAsPackageName(pkgInfo: PackageInfo): Boolean {
            items.find { it.name == pkgInfo.packageName }?.let {
                return it.label == pkgInfo.packageName
            } ?: return false
        }

        fun updateLabel(pkgInfo: PackageInfo, label: String, locale: Locale) {
            items.find { it.name == pkgInfo.packageName }?.let {
                it.label = label; it.locale = locale; it.ignore = false
            }
        }

        fun check(list: Set<String>) {
            items.forEach {
                it.checked = list.contains(it.name)
            }
        }

        fun checkVisible() {
            items.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = true }
        }

        fun uncheckVisible() {
            items.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = false }
        }

        fun ignore(list: Set<String>) {
            items.forEach {
                if (list.contains(it.name))
                    it.ignore = true
            }
        }

        fun unignore(list: Set<String>) {
            items.forEach {
                if (list.contains(it.name))
                    it.ignore = false
            }
        }

        fun getSorted(): List<PlaceholderPackage> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    getSortedByCacheSize()
                else
                    getSortedByLabel()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun getSortedByCacheSize(): List<PlaceholderPackage> {
            return items.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenByDescending { it.stats?.cacheBytes ?: 0 }
                    .thenBy { it.label })
        }

        fun getSortedByLabel(): List<PlaceholderPackage> {
            return items.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenBy { it.label })
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getFilteredByCacheSize(minCacheBytes: Long): List<PlaceholderPackage> {
            val compareCacheBytes: (cacheBytes: Long?) -> Boolean = { cacheBytes ->
                cacheBytes?.let { v -> v >= minCacheBytes } ?: false
            }

            return items.filterNot { it.ignore }
                .onEach { it.visible = compareCacheBytes(it.stats?.cacheBytes) }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenByDescending { it.stats?.cacheBytes ?: 0 }
                    .thenBy { it.label })
        }

        fun getFilteredByName(text: String): List<PlaceholderPackage> {
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

        fun update(list: List<PlaceholderPackage>) {
            items = list
        }

        fun getVisible(): List<PlaceholderPackage> {
            return items.filter { it.visible }
        }

        fun getChecked(): List<PlaceholderPackage> {
            return items.filter { it.checked }
        }

        fun getPackageNames(): List<String> {
            return items.map { it.name }
        }

        fun getCheckedPackageNames(): List<String> {
            return items.filter { it.checked }.map { it.name }
        }

        fun isAllVisibleChecked(): Boolean {
            return items.filter { it.visible }.all { it.checked }
        }

        fun isAllVisibleUnchecked(): Boolean {
            return items.filter { it.visible }.none { it.checked }
        }

        fun find(pkgName: String): PlaceholderPackage? {
            return items.firstOrNull{ it.name == pkgName }
        }
    }

    data class PlaceholderPackage(val pkgInfo: PackageInfo, val name: String,
                                  var label: String, var locale: Locale,
                                  var stats: StorageStats?, var visible: Boolean,
                                  var checked: Boolean, var ignore: Boolean) {
        override fun toString(): String = name
    }
}