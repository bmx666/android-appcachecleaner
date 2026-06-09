package com.github.bmx666.appcachecleaner.ui.viewmodel

import android.app.usage.StorageStats
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bmx666.appcachecleaner.data.LocaleManager
import com.github.bmx666.appcachecleaner.data.PackageRepository
import com.github.bmx666.appcachecleaner.data.PackageSource
import com.github.bmx666.appcachecleaner.data.UserPrefCustomPackageListManager
import com.github.bmx666.appcachecleaner.data.UserPrefFilterManager
import com.github.bmx666.appcachecleaner.model.PlaceholderPackage
import com.github.bmx666.appcachecleaner.platform.DispatcherProvider
import com.github.bmx666.appcachecleaner.util.ByteFormatter
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

// Package metadata (stats + label) is IPC-bound, not CPU-bound, so a small fixed
// pool overlaps PackageManager/StorageStats round trips without flooding binder.
private const val METADATA_FETCH_CONCURRENCY = 32

@HiltViewModel
class PackageListViewModel @Inject constructor(
    private val userPrefCustomPackageListManager: UserPrefCustomPackageListManager,
    private val userPrefFilterManager: UserPrefFilterManager,
    private val localeManager: LocaleManager,
    private val repo: PackageRepository,
    private val packageSource: PackageSource,
    private val dispatchers: DispatcherProvider,
    @param:ApplicationContext private val context: Context,
): ViewModel()
{
    private enum class PackageSort {
        BY_SIZE,
        BY_LABEL,
    }

    // `exists` = already in the repository; `label` null = existing entry whose label
    // needs no refresh.
    private data class FetchedPackage(
        val pkgInfo: PackageInfo,
        val exists: Boolean,
        val stats: StorageStats?,
        val label: String?,
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    var progressText by mutableStateOf("")
    var progressValue by mutableFloatStateOf(0f)

    init {
        updateProgress(0, 0)
    }

    // Single source of truth: observe the repository directly (no mirrored slices).
    val pkgListChecked: StateFlow<Set<String>> = repo.checked
    val pkgListCurrentVisible: StateFlow<List<PlaceholderPackage>> = repo.visiblePackages

    // User-entered minimum cache size for display only (the actual filter lives in repo).
    @RequiresApi(Build.VERSION_CODES.O)
    private val _filterByCacheSize = MutableStateFlow(0L)
    @RequiresApi(Build.VERSION_CODES.O)
    val filterByCacheSizeString: StateFlow<String?> =
        _filterByCacheSize.map { bytes ->
            ByteFormatter.format(bytes, LocaleHelper.getCurrentLocale(context))
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null,
        )

    val checkedTotalCacheSizeString: StateFlow<String?> =
        repo.checkedTotalCacheBytes.map { bytes ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    when {
                        bytes > 0 ->
                            ByteFormatter.format(bytes, LocaleHelper.getCurrentLocale(context))
                        else -> null
                    }
                else -> null
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    @RequiresApi(Build.VERSION_CODES.O)
    fun filterByCacheSize(minCacheSizeBytes: Long) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isProcessing.value = true
                repo.applyFilterByCacheSize(minCacheSizeBytes)
                _filterByCacheSize.value = minCacheSizeBytes
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private var filterByNameJob: Job? = null

    fun filterByName(text: String) {
        filterByNameJob?.cancel()
        filterByNameJob = viewModelScope.launch(dispatchers.io) {
            try {
                _isProcessing.value = true
                repo.applyFilterByName(text)
            } finally {
                _isProcessing.value = false
            }
        }
        filterByNameJob?.start()
    }

    fun checkPackage(packageName: String, checked: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            repo.setChecked(packageName, checked)
        }
    }

    fun checkVisible() {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isProcessing.value = true
                repo.checkVisible()
                // use fake delay to reload list
                delay(250L)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun uncheckVisible() {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isProcessing.value = true
                repo.uncheckVisible()
                // use fake delay to reload list
                delay(250L)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveListOfIgnoredApps() {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isProcessing.value = true
                val pkgList = repo.getChecked()
                if (pkgList.isEmpty())
                    userPrefFilterManager.removeListOfIgnoredApps()
                else
                    userPrefFilterManager.setListOfIgnoredApps(pkgList)

                // use fake delay to reload list
                delay(250L)
                repo.applySortByLabel()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveCustomListOfApps(
        name: String,
        onSave: (String) -> Unit,
        onEmpty: () -> Unit,
    ) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isProcessing.value = true
                val pkgList = repo.getChecked()
                if (pkgList.isNotEmpty()) {
                    userPrefCustomPackageListManager.setCustomList(name, pkgList)
                    viewModelScope.launch(dispatchers.main) {
                        onSave(name)
                    }
                    // use fake delay to reload list
                    delay(250L)
                    repo.applySortByLabel()
                } else {
                    viewModelScope.launch(dispatchers.main) {
                        onEmpty()
                    }
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun loadUserApps() =
        process {
            processPackageList(
                pkgList = packageSource.getInstalledApps(
                    systemNotUpdated = false,
                    systemUpdated = true,
                    userOnly = true,
                ),
                minCacheSizeBytes = userPrefFilterManager.minCacheSizeBytes.firstOrNull(),
                hideDisabledApps = userPrefFilterManager.hideDisabledApps.firstOrNull(),
                hideIgnoredApps = userPrefFilterManager.hideIgnoredApps.firstOrNull(),
                listOfIgnoredApps = userPrefFilterManager.listOfIgnoredApps.firstOrNull(),
            )
        }

    fun loadSystemApps() =
        process {
            processPackageList(
                pkgList = packageSource.getInstalledApps(
                    systemNotUpdated = true,
                    systemUpdated = false,
                    userOnly = false,
                ),
                minCacheSizeBytes = userPrefFilterManager.minCacheSizeBytes.firstOrNull(),
                hideDisabledApps = userPrefFilterManager.hideDisabledApps.firstOrNull(),
                hideIgnoredApps = userPrefFilterManager.hideIgnoredApps.firstOrNull(),
                listOfIgnoredApps = userPrefFilterManager.listOfIgnoredApps.firstOrNull(),
            )
        }

    fun loadAllApps() =
        process {
            processPackageList(
                pkgList = packageSource.getInstalledApps(
                    systemNotUpdated = true,
                    systemUpdated = true,
                    userOnly = true,
                ),
                minCacheSizeBytes = userPrefFilterManager.minCacheSizeBytes.firstOrNull(),
                hideDisabledApps = userPrefFilterManager.hideDisabledApps.firstOrNull(),
                hideIgnoredApps = userPrefFilterManager.hideIgnoredApps.firstOrNull(),
                listOfIgnoredApps = userPrefFilterManager.listOfIgnoredApps.firstOrNull(),
            )
        }

    fun loadDisabledApps() =
        process {
            processPackageList(
                pkgList = packageSource.getDisabledApps(),
            )
        }

    fun loadIgnoredApps() =
        process {
            processPackageList(
                pkgList = packageSource.getInstalledApps(
                    systemNotUpdated = true,
                    systemUpdated = true,
                    userOnly = true,
                ),
                checkedPkgList = userPrefFilterManager.listOfIgnoredApps.firstOrNull(),
                requestStats = false,
                sort = PackageSort.BY_LABEL,
            )
        }

    fun loadCustomListAppsAdd() =
        process {
            processPackageList(
                pkgList = packageSource.getInstalledApps(
                    systemNotUpdated = true,
                    systemUpdated = true,
                    userOnly = true,
                ),
                requestLabel = true,
                requestStats = false,
                sort = PackageSort.BY_LABEL,
            )
        }

    fun loadCustomListAppsEdit(name: String) =
        process {
            processPackageList(
                pkgList = packageSource.getInstalledApps(
                    systemNotUpdated = true,
                    systemUpdated = true,
                    userOnly = true,
                ),
                checkedPkgList = userPrefCustomPackageListManager.getCustomList(name).firstOrNull(),
                requestLabel = true,
                requestStats = false,
                sort = PackageSort.BY_LABEL,
            )
        }

    fun loadCustomListAppsFilter(name: String) =
        process {
            processPackageList(
                pkgList = packageSource.getInstalledApps(
                    systemNotUpdated = true,
                    systemUpdated = true,
                    userOnly = true,
                ),
                checkedPkgList = userPrefCustomPackageListManager.getCustomList(name).firstOrNull(),
                hideUncheckedApps = true,
                requestLabel = true,
                requestStats = true,
                sort = PackageSort.BY_LABEL,
            )
        }

    private suspend fun processPackageList(
        pkgList: List<PackageInfo>,
        checkedPkgList: Set<String>? = null,
        minCacheSizeBytes: Long? = null,
        hideUncheckedApps: Boolean? = null,
        hideDisabledApps: Boolean? = null,
        hideIgnoredApps: Boolean? = null,
        listOfIgnoredApps: Set<String>? = null,
        requestStats: Boolean = true,
        requestLabel: Boolean = true,
        sort: PackageSort = PackageSort.BY_SIZE)
    {
        val currentLocale = localeManager.currentLocale.value

        val total = pkgList.size
        updateProgress(0, total)

        repo.reset()

        // Fetch per-package stats + label in parallel, then mutate the repository
        // serially. The parallel pass only READS the repository
        // (contains/isLabelAsPackageName/isSameLabelLocale) and never mutates it, so
        // existence + label-fetch decisions stay stable across tasks. Insertion order is
        // irrelevant: the list is sorted afterwards by the applyXxx call. Heavy IO runs
        // outside the repository mutex.
        val semaphore = Semaphore(METADATA_FETCH_CONCURRENCY)
        val progress = AtomicInteger(0)

        val fetched = coroutineScope {
            pkgList.map { pkgInfo ->
                async {
                    // skip disabled app
                    if (hideDisabledApps == true &&
                        (pkgInfo.applicationInfo?.enabled == false))
                        return@async null

                    if (hideIgnoredApps == true &&
                        listOfIgnoredApps?.contains(pkgInfo.packageName) == true)
                        return@async null

                    if (hideUncheckedApps == true &&
                        checkedPkgList?.contains(pkgInfo.packageName) == false)
                        return@async null

                    semaphore.withPermit {
                        val stats =
                            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && requestStats)
                                packageSource.getStorageStats(pkgInfo)
                            else
                                null

                        val exists = repo.contains(pkgInfo)
                        // null label => existing entry whose label needs no refresh.
                        val label: String? =
                            if (!exists) {
                                if (requestLabel)
                                    packageSource.getApplicationLabel(pkgInfo)
                                else
                                    pkgInfo.packageName
                            } else if (requestLabel &&
                                (repo.isLabelAsPackageName(pkgInfo) ||
                                 !repo.isSameLabelLocale(pkgInfo, currentLocale))) {
                                packageSource.getApplicationLabel(pkgInfo)
                            } else {
                                null
                            }

                        FetchedPackage(pkgInfo, exists, stats, label)
                    }.also {
                        updateProgress(progress.incrementAndGet(), total)
                    }
                }
            }.awaitAll().filterNotNull()
        }

        fetched.forEach { f ->
            if (f.exists) {
                repo.updateStats(f.pkgInfo, f.stats)
                f.label?.let { repo.updateLabel(f.pkgInfo, it, currentLocale) }
            } else {
                repo.add(
                    f.pkgInfo, f.label ?: f.pkgInfo.packageName, currentLocale, f.stats)
            }
        }

        updateProgress(total, total)

        checkedPkgList?.let {
            repo.setCheckedBatch(checkedPkgList)
        }

        // Apply the active view; this recomputes the visible list and emits the flows.
        when (sort) {
            PackageSort.BY_SIZE ->
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        repo.applyFilterByCacheSize(minCacheSizeBytes ?: 0L)
                    else ->
                        repo.applySortByLabel()
                }
            PackageSort.BY_LABEL ->
                repo.applySortByLabel()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // reset custom filter by cache size
            _filterByCacheSize.value = 0L
        }
    }

    private fun process(process: suspend () -> Unit) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isLoading.value = true
                updateProgress(0, 0)
                process()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateProgress(index: Int, total: Int) {
        progressValue =
            if (total == 0) 0f
            else index.toFloat() / total.toFloat()
        progressText = "$index / $total"
    }
}
