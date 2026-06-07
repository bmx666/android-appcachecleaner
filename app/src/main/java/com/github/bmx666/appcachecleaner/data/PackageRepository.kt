package com.github.bmx666.appcachecleaner.data

import android.app.usage.StorageStats
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.bmx666.appcachecleaner.model.PlaceholderPackage
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// App-process-scoped owner of the package list and its UI view state. @Singleton (NOT
// ViewModel-scoped): the list must outlive ViewModel/Activity recreation across the
// Settings/accessibility excursion, and the master list stays warm for the whole
// process because re-collecting per-package stats is minutes-long IO on slow devices.
//
// Single source of truth: this repo owns the active filter/sort spec AND emits the
// derived StateFlows. ViewModels observe; they never re-push slices. One Mutex guards
// all mutable state (master list, index, selection, view spec). Mutex is NOT reentrant:
// public suspend fns lock; private *Locked helpers must already hold the lock.
@Singleton
class PackageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()

    // Master list: every loaded package. Never evicted (warm cache).
    private val items = mutableListOf<PlaceholderPackage>()
    // Name -> object, strict 1:1 with items (add is idempotent first-wins). O(1) lookup.
    private val index = hashMapOf<String, PlaceholderPackage>()
    // Selection source of truth, decoupled from the heavy package objects.
    private val checkedNames = hashSetOf<String>()

    // Active view: which filter/sort produced the current visible list.
    private sealed interface ViewSpec {
        data object SortByLabel : ViewSpec
        data class FilterByCacheSize(val minBytes: Long) : ViewSpec
        data class FilterByName(val text: String) : ViewSpec
    }
    private var viewSpec: ViewSpec = ViewSpec.SortByLabel
    // Last computed view (all non-ignored, sorted; visible flag marks the shown subset).
    private var currentList: List<PlaceholderPackage> = emptyList()

    private val _visiblePackages = MutableStateFlow<List<PlaceholderPackage>>(emptyList())
    val visiblePackages: StateFlow<List<PlaceholderPackage>> = _visiblePackages.asStateFlow()

    private val _checked = MutableStateFlow<Set<String>>(emptySet())
    val checked: StateFlow<Set<String>> = _checked.asStateFlow()

    private val _checkedTotalCacheBytes = MutableStateFlow(0L)
    val checkedTotalCacheBytes: StateFlow<Long> = _checkedTotalCacheBytes.asStateFlow()

    // ---- build phase (called during a load; no emit until the final applyXxx) ----

    suspend fun reset() = mutex.withLock {
        items.forEach {
            it.ignore = true
            it.visible = true
        }
        checkedNames.clear()
    }

    suspend fun contains(pkgInfo: PackageInfo): Boolean = mutex.withLock {
        index.containsKey(pkgInfo.packageName)
    }

    suspend fun add(pkgInfo: PackageInfo, label: String, locale: Locale,
                    stats: StorageStats?) = mutex.withLock {
        // Idempotent first-wins: never create a second entry for an existing name
        // (multi-profile / cloned apps), keeping items and index in exact 1:1 sync.
        if (index.containsKey(pkgInfo.packageName))
            return@withLock
        val pkg = PlaceholderPackage(
            pkgInfo = pkgInfo,
            name = pkgInfo.packageName,
            label = label,
            locale = locale,
            stats = stats,
            visible = true,
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
        index[pkgInfo.packageName]?.locale == locale
    }

    suspend fun isLabelAsPackageName(pkgInfo: PackageInfo): Boolean = mutex.withLock {
        index[pkgInfo.packageName]?.let { it.label == pkgInfo.packageName } ?: false
    }

    suspend fun updateLabel(pkgInfo: PackageInfo, label: String, locale: Locale) = mutex.withLock {
        index[pkgInfo.packageName]?.let {
            it.label = label; it.locale = locale; it.ignore = false
        }
        Unit
    }

    // Replace selection with the given names (intersected with known packages).
    // Build-phase helper: does NOT emit; the trailing applyXxx recomputes + emits.
    suspend fun setCheckedBatch(list: Set<String>) = mutex.withLock {
        checkedNames.clear()
        list.forEach { if (index.containsKey(it)) checkedNames.add(it) }
    }

    // ---- selection (emit) ----

    suspend fun setChecked(packageName: String, checked: Boolean) = mutex.withLock {
        if (checked) checkedNames.add(packageName) else checkedNames.remove(packageName)
        recomputeLocked()
    }

    suspend fun checkVisible() = mutex.withLock {
        currentList.filter { it.visible }.forEach { checkedNames.add(it.name) }
        recomputeLocked()
    }

    suspend fun uncheckVisible() = mutex.withLock {
        currentList.filter { it.visible }.forEach { checkedNames.remove(it.name) }
        recomputeLocked()
    }

    suspend fun getChecked(): Set<String> = mutex.withLock {
        checkedNames.toSet()
    }

    // ---- view spec (emit) ----

    suspend fun applySortByLabel() = mutex.withLock {
        viewSpec = ViewSpec.SortByLabel
        recomputeLocked()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun applyFilterByCacheSize(minCacheBytes: Long) = mutex.withLock {
        viewSpec = ViewSpec.FilterByCacheSize(minCacheBytes)
        recomputeLocked()
    }

    suspend fun applyFilterByName(text: String) = mutex.withLock {
        viewSpec = ViewSpec.FilterByName(text)
        recomputeLocked()
    }

    // ---- post-clean write-back ----

    // Re-read stats of the currently checked packages and write them back into the
    // master list (warm cache stays current without a full refetch), returning total
    // bytes cleaned. One batch settle delay first: the StorageStats quota path lags
    // after a cache drop, so an immediate read can still report the pre-clean size.
    // Heavy IO runs OUTSIDE the mutex; the lock is taken only to snapshot then write.
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun refreshStatsAfterCacheClean(): Long {
        delay(POST_CLEAN_SETTLE_MS)

        // Snapshot the checked-and-shown packages (parity with the old Current.getChecked).
        val targets = mutex.withLock {
            currentList.filter { checkedNames.contains(it.name) }
                .map { it.pkgInfo to it.stats }
        }

        var cleanedBytes = 0L
        val fresh = HashMap<String, StorageStats?>(targets.size)
        targets.forEach { (pkgInfo, oldStats) ->
            val newStats = PackageManagerHelper.getStorageStats(context, pkgInfo)
            cleanedBytes += PackageManagerHelper.getCacheSizeDiff(oldStats, newStats)
            fresh[pkgInfo.packageName] = newStats
        }

        mutex.withLock {
            fresh.forEach { (name, stats) -> index[name]?.stats = stats }
            recomputeLocked()
        }
        return cleanedBytes
    }

    // ---- internals (require the lock) ----

    private fun recomputeLocked() {
        currentList = computeViewLocked()
        _visiblePackages.value = currentList.filter { it.visible }
        _checked.value = checkedNames.toSet()
        _checkedTotalCacheBytes.value = checkedTotalCacheBytesLocked()
    }

    private fun computeViewLocked(): List<PlaceholderPackage> {
        val base = items.filterNot { it.ignore }
        return when (val spec = viewSpec) {
            ViewSpec.SortByLabel ->
                base.onEach { it.visible = true }.sortedByLabelLocked()
            is ViewSpec.FilterByName -> {
                val needle = spec.text.trim().lowercase()
                val match: (String) -> Boolean =
                    if (needle.isEmpty()) { _ -> true }
                    else { label -> label.lowercase().contains(needle) }
                base.onEach { it.visible = match(it.label) }.sortedByLabelLocked()
            }
            is ViewSpec.FilterByCacheSize ->
                // FilterByCacheSize is only ever set via applyFilterByCacheSize (O+),
                // but guard inline so lint can prove the getCacheSize() calls are safe.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    base.onEach { it.visible = (it.getCacheSize() >= spec.minBytes) }
                        .sortedWith(compareByDescending<PlaceholderPackage> { it.getCacheSize() }
                            .thenBy { it.label })
                else
                    base.onEach { it.visible = true }.sortedByLabelLocked()
        }
    }

    private fun List<PlaceholderPackage>.sortedByLabelLocked(): List<PlaceholderPackage> =
        sortedWith(compareBy<PlaceholderPackage> { !checkedNames.contains(it.name) }
            .thenBy { it.label })

    private fun checkedTotalCacheBytesLocked(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            items.filter { checkedNames.contains(it.name) }.sumOf { it.getCacheSize() }
        else 0L

    companion object {
        // One batch settle before re-reading post-clean stats (quota path lags).
        private const val POST_CLEAN_SETTLE_MS = 1000L
    }
}
