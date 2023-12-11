package com.github.bmx666.appcachecleaner.ui.viewmodel

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
import com.github.bmx666.appcachecleaner.data.UserPrefCustomPackageListManager
import com.github.bmx666.appcachecleaner.data.UserPrefFilterManager
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.toFormattedString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.springframework.util.unit.DataSize
import javax.inject.Inject

@HiltViewModel
class PackageListViewModel @Inject constructor(
    private val userPrefCustomPackageListManager: UserPrefCustomPackageListManager,
    private val userPrefFilterManager: UserPrefFilterManager,
    @ApplicationContext private val context: Context,
): ViewModel()
{
    private enum class PackageSort {
        BY_SIZE,
        BY_LABEL,
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    var progressText by mutableStateOf("")
    var progressValue by mutableFloatStateOf(0f)

    init {
        updateProgress(0, 0)
    }

    private val _pkgListChecked = MutableStateFlow<Set<String>>(emptySet())
    val pkgListChecked: StateFlow<Set<String>> = _pkgListChecked

    private val _pkgListCurrentVisible = MutableStateFlow<List<PlaceholderContent.PlaceholderPackage>>(emptyList())
    val pkgListCurrentVisible: StateFlow<List<PlaceholderContent.PlaceholderPackage>> = _pkgListCurrentVisible

    @RequiresApi(Build.VERSION_CODES.O)
    private val _filterByCacheSize = MutableStateFlow(0L)
    @RequiresApi(Build.VERSION_CODES.O)
    val filterByCacheSizeString: StateFlow<String?> =
        _filterByCacheSize.map { bytes ->
            DataSize.ofBytes(bytes).toFormattedString(context)
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null,
        )

    private val _checkedTotalCacheSize = MutableStateFlow(0L)
    val checkedTotalCacheSizeString: StateFlow<String?> =
        _checkedTotalCacheSize.map { bytes ->
            bytes.let {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        when {
                            bytes > 0 ->
                                DataSize.ofBytes(bytes).toFormattedString(context)
                            else -> null
                        }
                    else -> null
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            null
        )

    @RequiresApi(Build.VERSION_CODES.O)
    fun filterByCacheSize(minCacheSizeBytes: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getFilteredByCacheSize(minCacheSizeBytes)
                )
                _filterByCacheSize.value = minCacheSizeBytes
                _pkgListCurrentVisible.value = PlaceholderContent.Current.getVisible()
                _pkgListChecked.value = PlaceholderContent.All.getChecked()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private var filterByNameJob: Job? = null

    fun filterByName(text: String) {
        filterByNameJob?.cancel()
        filterByNameJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getFilteredByName(text))
                _pkgListCurrentVisible.value = PlaceholderContent.Current.getVisible()
                _pkgListChecked.value = PlaceholderContent.All.getChecked()
            } finally {
                _isProcessing.value = false
            }
        }
        filterByNameJob?.start()
    }

    fun checkPackage(packageName: String, checked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            PlaceholderContent.All.check(packageName, checked)
            _pkgListChecked.value = PlaceholderContent.All.getChecked()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                _checkedTotalCacheSize.value = PlaceholderContent.Current.getCheckedTotalCacheSize()
        }
    }

    fun checkVisible() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                PlaceholderContent.All.checkVisible()
                // use fake delay to reload list
                delay(250L)

                _pkgListCurrentVisible.value =
                    PlaceholderContent.Current.getVisible()
                _pkgListChecked.value =
                    PlaceholderContent.All.getChecked()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    _checkedTotalCacheSize.value =
                        PlaceholderContent.Current.getCheckedTotalCacheSize()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun uncheckVisible() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                PlaceholderContent.All.uncheckVisible()
                // use fake delay to reload list
                delay(250L)

                _pkgListCurrentVisible.value =
                    PlaceholderContent.Current.getVisible()
                _pkgListChecked.value =
                    PlaceholderContent.All.getChecked()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    _checkedTotalCacheSize.value =
                        PlaceholderContent.Current.getCheckedTotalCacheSize()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveListOfIgnoredApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                val pkgList = PlaceholderContent.All.getChecked()
                if (pkgList.isEmpty())
                    userPrefFilterManager.removeListOfIgnoredApps()
                else
                    userPrefFilterManager.setListOfIgnoredApps(pkgList)

                // use fake delay to reload list
                delay(250L)
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getSortedByLabel())
                _pkgListCurrentVisible.value =
                    PlaceholderContent.Current.getVisible()
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                val pkgList = PlaceholderContent.All.getChecked()
                if (pkgList.isNotEmpty()) {
                    userPrefCustomPackageListManager.setCustomList(name, pkgList)
                    viewModelScope.launch(Dispatchers.Main) {
                        onSave(name)
                    }
                    // use fake delay to reload list
                    delay(250L)
                    PlaceholderContent.Current.update(
                        PlaceholderContent.All.getSortedByLabel())
                    _pkgListCurrentVisible.value =
                        PlaceholderContent.Current.getVisible()
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
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
                pkgList = PackageManagerHelper.getInstalledApps(
                    context = context,
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
                pkgList = PackageManagerHelper.getInstalledApps(
                    context = context,
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
                pkgList = PackageManagerHelper.getInstalledApps(
                    context = context,
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
                pkgList = PackageManagerHelper.getDisabledApps(context),
            )
        }

    fun loadIgnoredApps() =
        process {
            processPackageList(
                pkgList = PackageManagerHelper.getInstalledApps(
                    context = context,
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
                pkgList = PackageManagerHelper.getInstalledApps(
                    context = context,
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
                pkgList = PackageManagerHelper.getInstalledApps(
                    context = context,
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

    private suspend fun processPackageList(
        pkgList: ArrayList<PackageInfo>,
        checkedPkgList: Set<String>? = null,
        minCacheSizeBytes: Long? = null,
        hideDisabledApps: Boolean? = null,
        hideIgnoredApps: Boolean? = null,
        listOfIgnoredApps: Set<String>? = null,
        requestStats: Boolean = true,
        requestLabel: Boolean = true,
        sort: PackageSort = PackageSort.BY_SIZE)
    {
        val locale = LocaleHelper.getCurrentLocale(context)

        val total = pkgList.size
        updateProgress(0, total)

        PlaceholderContent.All.reset()

        pkgList.forEachIndexed { index, pkgInfo ->
            updateProgress(index, total)

            // skip disabled app
            if (hideDisabledApps == true &&
                (pkgInfo.applicationInfo?.enabled == false))
                return@forEachIndexed

            if (hideIgnoredApps == true &&
                listOfIgnoredApps?.contains(pkgInfo.packageName) == true)
                return@forEachIndexed

            val stats =
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  && requestStats)
                    PackageManagerHelper.getStorageStats(context, pkgInfo)
                else
                    null

            if (PlaceholderContent.All.contains(pkgInfo)) {
                PlaceholderContent.All.updateStats(pkgInfo, stats)
                if (requestLabel) {
                    if (PlaceholderContent.All.isLabelAsPackageName(pkgInfo) ||
                        !PlaceholderContent.All.isSameLabelLocale(pkgInfo, locale)) {
                        val label = PackageManagerHelper.getApplicationLabel(context, pkgInfo)
                        PlaceholderContent.All.updateLabel(pkgInfo, label, locale)
                    }
                }
            } else {
                val label =
                    if (requestLabel) {
                        PackageManagerHelper.getApplicationLabel(context, pkgInfo)
                    } else {
                        pkgInfo.packageName
                    }

                PlaceholderContent.All.add(pkgInfo, label, locale, stats)
            }
        }

        updateProgress(total, total)

        checkedPkgList?.let {
            PlaceholderContent.All.check(checkedPkgList)
        }

        when (sort) {
            PackageSort.BY_SIZE ->
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        PlaceholderContent.Current.update(
                            PlaceholderContent.All.getFilteredByCacheSize(minCacheSizeBytes ?: 0L)
                        )
                    else ->
                        PlaceholderContent.Current.update(
                            PlaceholderContent.All.getSorted())
                }
            PackageSort.BY_LABEL ->
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getSortedByLabel())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // reset custom filter by cache size
            _filterByCacheSize.value = 0L
        }

        _pkgListCurrentVisible.value = PlaceholderContent.Current.getVisible()
        _pkgListChecked.value = PlaceholderContent.All.getChecked()
    }

    private fun process(process: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
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