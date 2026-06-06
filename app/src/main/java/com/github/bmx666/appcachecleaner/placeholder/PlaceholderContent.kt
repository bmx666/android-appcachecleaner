package com.github.bmx666.appcachecleaner.placeholder

import android.app.usage.StorageStats
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.bmx666.appcachecleaner.util.getInternalCacheSize
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

object PlaceholderContent {

    // Single shared lock guards both All.items and Current.items. Mutex is NOT
    // reentrant: only leaf accessors lock; dispatch-only fns (getSorted) must not,
    // or a fn locking then calling another locked fn would deadlock.
    private val mutex = Mutex()

    object All {
        internal val items by lazy { mutableListOf<PlaceholderPackage>() }
        // Name -> package index. Holds the SAME objects as items, kept in strict 1:1
        // sync: add() is the only structural mutation in All (no remove/clear), and it
        // is idempotent (first-wins), so a name maps to exactly one items entry. Field
        // mutations through either view are visible in both. Guarded by the shared mutex
        // like items. Turns per-name lookups from O(N) find to O(1).
        private val index by lazy { hashMapOf<String, PlaceholderPackage>() }

        suspend fun reset() = mutex.withLock {
            items.forEach {
                it.ignore = true
                it.visible = true
                it.checked = false
            }
        }

        suspend fun contains(pkgInfo: PackageInfo): Boolean = mutex.withLock {
            index.containsKey(pkgInfo.packageName)
        }

        suspend fun add(pkgInfo: PackageInfo, label: String, locale: Locale,
                    stats: StorageStats?) = mutex.withLock {
            // Idempotent + first-wins: never create a second items entry for a name
            // already present. Keeps items and index in exact 1:1 sync even if the
            // package source yields duplicate names (multi-profile / cloned apps), so
            // updateStats/updateLabel via index[name] always hit the one displayed obj.
            if (index.containsKey(pkgInfo.packageName))
                return@withLock
            val pkg = PlaceholderPackage(
                pkgInfo = pkgInfo,
                name = pkgInfo.packageName,
                label = label,
                locale = locale,
                stats = stats,
                visible = true,
                checked = false,
                ignore = false)
            items.add(pkg)
            index[pkg.name] = pkg
            Unit
        }

        suspend fun updateStats(pkgInfo: PackageInfo, stats: StorageStats?) = mutex.withLock {
            index[pkgInfo.packageName]?.let {
                it.stats = stats; it.ignore = false
            }
            Unit
        }

        suspend fun isSameLabelLocale(pkgInfo: PackageInfo, locale: Locale): Boolean = mutex.withLock {
            index[pkgInfo.packageName]?.let {
                return@withLock it.locale == locale
            } ?: return@withLock false
        }

        suspend fun isLabelAsPackageName(pkgInfo: PackageInfo): Boolean = mutex.withLock {
            index[pkgInfo.packageName]?.let {
                return@withLock it.label == pkgInfo.packageName
            } ?: return@withLock false
        }

        suspend fun updateLabel(pkgInfo: PackageInfo, label: String, locale: Locale) = mutex.withLock {
            index[pkgInfo.packageName]?.let {
                it.label = label; it.locale = locale; it.ignore = false
            }
            Unit
        }

        suspend fun check(packageName: String, checked: Boolean) = mutex.withLock {
            // O(1) single-row toggle via index; 1:1 sync guarantees one obj per name.
            index[packageName]?.let { it.checked = checked }
            Unit
        }

        suspend fun check(list: Set<String>) = mutex.withLock {
            items.forEach { it.checked = list.contains(it.name) }
        }

        suspend fun checkVisible() = mutex.withLock {
            items.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = true }
        }

        suspend fun uncheckVisible() = mutex.withLock {
            items.filterNot { it.ignore }.filter { it.visible }.forEach { it.checked = false }
        }

        suspend fun getChecked(): Set<String> = mutex.withLock {
            items.filter { it.checked }.map { it.name }.toSet()
        }

        suspend fun getSorted(): List<PlaceholderPackage> {
            // Dispatch only - delegates lock. Do NOT lock here (non-reentrant Mutex).
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    getSortedByCacheSize()
                else
                    getSortedByLabel()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private suspend fun getSortedByCacheSize(): List<PlaceholderPackage> = mutex.withLock {
            items.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareByDescending<PlaceholderPackage> { it.getCacheSize() }
                    .thenBy { it.label })
        }

        suspend fun getSortedByLabel(): List<PlaceholderPackage> = mutex.withLock {
            items.filterNot { it.ignore }
                .onEach { it.visible = true }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenBy { it.label })
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getFilteredByCacheSize(minCacheBytes: Long): List<PlaceholderPackage> = mutex.withLock {
            val compareCacheBytes: (cacheBytes: Long?) -> Boolean = { cacheBytes ->
                cacheBytes?.let { v -> v >= minCacheBytes } ?: false
            }

            items.filterNot { it.ignore }
                .onEach { it.visible = compareCacheBytes(it.getCacheSize()) }
                .sortedWith(compareByDescending<PlaceholderPackage> { it.getCacheSize() }
                    .thenBy { it.label })
        }

        suspend fun getFilteredByName(text: String): List<PlaceholderPackage> = mutex.withLock {
            val finalText = text.trim().lowercase()
            val finalTextIsNotEmpty = finalText.isNotEmpty()
            val compareLabel: (label: String) -> Boolean = { label ->
                if (finalTextIsNotEmpty)
                    label.lowercase().contains(finalText)
                else true
            }

            items.filterNot { it.ignore }
                .onEach { it.visible = compareLabel(it.label) }
                .sortedWith(compareBy<PlaceholderPackage> { !it.checked }
                    .thenBy { it.label })
        }
    }

    object Current {
        internal val items by lazy { mutableListOf<PlaceholderPackage>() }

        suspend fun update(list: List<PlaceholderPackage>) = mutex.withLock {
            items.clear()
            items.addAll(list)
            Unit
        }

        suspend fun getVisible(): List<PlaceholderPackage> = mutex.withLock {
            items.filter { it.visible }
        }

        suspend fun getChecked(): List<PlaceholderPackage> = mutex.withLock {
            items.filter { it.checked }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getCheckedTotalCacheSize(): Long = mutex.withLock {
            // Reads All.items, so guarded by the same shared mutex.
            All.items.filter { it.checked }.sumOf { it.getCacheSize() }
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
