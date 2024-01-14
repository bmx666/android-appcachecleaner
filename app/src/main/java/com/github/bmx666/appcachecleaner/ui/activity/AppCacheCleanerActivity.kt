package com.github.bmx666.appcachecleaner.ui.activity

import android.app.SearchManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.FileUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.databinding.ActivityMainBinding
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.service.CacheCleanerTileService
import com.github.bmx666.appcachecleaner.ui.dialog.CustomListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.FilterListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.PermissionDialogBuilder
import com.github.bmx666.appcachecleaner.ui.fragment.HelpFragment
import com.github.bmx666.appcachecleaner.ui.fragment.PackageListFragment
import com.github.bmx666.appcachecleaner.ui.fragment.SettingsFragment
import com.github.bmx666.appcachecleaner.util.ActivityHelper
import com.github.bmx666.appcachecleaner.util.ExtraSearchTextHelper
import com.github.bmx666.appcachecleaner.util.IIntentActivityCallback
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerActivityHelper
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.PermissionChecker
import com.github.bmx666.appcachecleaner.util.TileRequestResult
import com.github.bmx666.appcachecleaner.util.toFormattedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.util.unit.DataSize
import java.io.File
import java.util.Locale

class AppCacheCleanerActivity : AppCompatActivity(), IIntentActivityCallback {

    companion object {
        const val ARG_DISPLAY_TEXT = "display-text"
        const val FRAGMENT_CONTAINER_VIEW_TAG = "fragment-container-view-tag"
    }

    private lateinit var binding: ActivityMainBinding
    private var customListName: String? = null
    private var minCacheSizeStr: String? = null

    private lateinit var onMenuHideAll: () -> Unit
    private lateinit var onMenuShowMain: () -> Unit
    private lateinit var onMenuShowFilter: () -> Unit
    private lateinit var onMenuShowSearch: () -> Unit

    private lateinit var localBroadcastManager: LocalBroadcastManagerActivityHelper

    private var calculationCleanedCacheJob: Job? = null
    private var loadingPkgListJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SharedPreferencesManager.UI.getNightMode(this))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        localBroadcastManager = LocalBroadcastManagerActivityHelper(this, this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    this@AppCacheCleanerActivity.handleOnBackPressed()
                }
            }
        )

        binding.btnCleanUserAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            preparePackageList(null,
                PackageManagerHelper.getInstalledApps(
                    context = this,
                    systemNotUpdated = false,
                    systemUpdated = true,
                    userOnly = true,
                ),
                false
            )
        }

        binding.btnCleanSystemAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            preparePackageList(null,
                PackageManagerHelper.getInstalledApps(
                    context = this,
                    systemNotUpdated = true,
                    systemUpdated = false,
                    userOnly = false,
                ),
                false
            )
        }

        binding.btnCleanAllAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            preparePackageList(null,
                PackageManagerHelper.getInstalledApps(
                    context = this,
                    systemNotUpdated = true,
                    systemUpdated = true,
                    userOnly = true,
                ),
                false
            )
        }

        binding.btnStartStopService.setOnClickListener {
            if (PermissionChecker.checkAccessibilityPermission(this))
                localBroadcastManager.disableAccessibilityService()
            else
                PermissionDialogBuilder.buildAccessibilityPermissionDialog(this).show()
        }

        binding.btnCloseApp.setOnClickListener {
            if (PermissionChecker.checkAccessibilityPermission(this))
                localBroadcastManager.disableAccessibilityService()
            finish()
        }

        binding.fabCleanCache.setOnClickListener {
            val pkgList = PlaceholderContent.Current.getCheckedPackageNames().toMutableList()
            startCleanCache(pkgList)
        }

        binding.fabCheckAllApps.setOnClickListener {
            when (
                if (PlaceholderContent.Current.isAllVisibleChecked())
                    "uncheck"
                else if (PlaceholderContent.Current.isAllVisibleUnchecked())
                    "check"
                else
                    binding.fabCheckAllApps.tag
            ) {
                "uncheck" -> {
                    binding.fabCheckAllApps.tag = "check"
                    binding.fabCheckAllApps.contentDescription =
                        getString(R.string.description_apps_all_check)
                    PlaceholderContent.All.uncheckVisible()
                }
                "check" -> {
                    binding.fabCheckAllApps.tag = "uncheck"
                    binding.fabCheckAllApps.contentDescription =
                        getString(R.string.description_apps_all_uncheck)
                    PlaceholderContent.All.checkVisible()
                }
            }

            supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                ?.let { fragment ->
                    if (fragment is PackageListFragment)
                        fragment.refreshAdapter()
                }
        }

        binding.fabCustomListOk.setOnClickListener {
            val checkedPkgList = PlaceholderContent.Current.getCheckedPackageNames().toSet()
            if (checkedPkgList.isEmpty()) {
                Toast.makeText(this,
                    R.string.toast_custom_list_add_list_empty,
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            customListName?.let { name ->
                SharedPreferencesManager.PackageList.save(this, name, checkedPkgList)
                Toast.makeText(this,
                    getString(R.string.toast_custom_list_has_been_saved, name),
                    Toast.LENGTH_SHORT).show()
            }
            handleOnBackPressed()
        }

        binding.fabCustomListCancel.setOnClickListener {
            handleOnBackPressed()
        }

        binding.btnCleanCustomListAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            CustomListDialogBuilder.buildCleanCacheDialog(this) { name ->
                name ?: return@buildCleanCacheDialog

                preparePackageList(name,
                    PackageManagerHelper.getCustomInstalledApps(
                        context = this,
                        pkgList = SharedPreferencesManager.PackageList.get(this, name)
                    ),
                    true
                )
            }.show()
        }

        if (calculationCleanedCacheJob?.isActive != true)
            updateMainText(intent.getCharSequenceExtra(ARG_DISPLAY_TEXT))

        checkRequestAddTileService()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)

        onMenuHideAll = {
            menu.findItem(R.id.menu_help).isVisible = false
            menu.findItem(R.id.menu_settings).isVisible = false
            menu.findItem(R.id.menu_filter).isVisible = false
            menu.findItem(R.id.menu_search).apply {
                if (isActionViewExpanded)
                    collapseActionView()
                isVisible = false
            }
        }

        onMenuShowMain = {
            menu.findItem(R.id.menu_help).isVisible = true
            menu.findItem(R.id.menu_settings).isVisible = true
            menu.findItem(R.id.menu_filter).isVisible = false
            menu.findItem(R.id.menu_search).apply {
                if (isActionViewExpanded)
                    collapseActionView()
                isVisible = false
            }
        }

        onMenuShowFilter = {
            menu.findItem(R.id.menu_help).isVisible = false
            menu.findItem(R.id.menu_settings).isVisible = false
            menu.findItem(R.id.menu_filter).isVisible =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            menu.findItem(R.id.menu_search).apply {
                if (isActionViewExpanded)
                    collapseActionView()
                isVisible = false
            }
        }

        onMenuShowSearch = {
            menu.findItem(R.id.menu_help).isVisible = false
            menu.findItem(R.id.menu_settings).isVisible = false
            menu.findItem(R.id.menu_filter).isVisible = false
            menu.findItem(R.id.menu_search).apply {
                if (isActionViewExpanded)
                    collapseActionView()
                isVisible = true
            }
        }

        val searchView = menu.findItem(R.id.menu_search)?.actionView as SearchView?

        searchView?.apply {
            val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            setIconifiedByDefault(false)

            setOnQueryTextListener(object: SearchView.OnQueryTextListener {

                private fun filter(text: String?) {
                    text ?: return
                    supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                        ?.let { fragment ->
                            if (fragment is PackageListFragment)
                                fragment.swapAdapterFilterByName(text)
                        }
                }

                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filter(newText)
                    return false
                }
            })
            setOnCloseListener {
                // unset ignore flag for all packages in the current list
                PlaceholderContent.All.unignore(
                    PlaceholderContent.Current.getPackageNames().toSet())
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getSortedByLabel())
                false
            }
        }

        restoreUI()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_help -> {
                showMenuFragment(HelpFragment.newInstance(), R.string.menu_item_help)
                true
            }
            R.id.menu_settings -> {
                showMenuFragment(SettingsFragment.newInstance(), R.string.menu_item_settings)
                true
            }
            R.id.menu_filter -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        updateExtraButtonsVisibility()
        updateStartStopServiceButton()
    }

    override fun onDestroy() {
        loadingPkgListJob?.cancel()
        calculationCleanedCacheJob?.cancel()
        localBroadcastManager.onDestroy()
        super.onDestroy()
    }

    private fun startCleanCache(pkgList: MutableList<String>) {
        setSettings()

        hideFragmentViews()
        showMainViews()

        pkgList.apply {
            // ignore empty list and show main screen
            if (isEmpty()) {
                updateMainText(null)
                return
            }

            // clear cache of app in the end to avoid issues
            if (contains(packageName)) {
                remove(packageName)
                // cache dir is using for log file in debug version
                // clean cache dir in release only
                if (!BuildConfig.DEBUG)
                    add(packageName)
            }
        }

        localBroadcastManager.sendPackageList(pkgList as ArrayList<String>)
    }

    private fun addPackageToPlaceholderContent(pkgInfoList: ArrayList<PackageInfo>,
                                               isCustomListClearOnly: Boolean) {
        val locale = LocaleHelper.getCurrentLocale(this)
        val hideDisabledApps = SharedPreferencesManager.Filter.getHideDisabledApps(this)

        var progressApps = 0
        val totalApps = pkgInfoList.size

        PlaceholderContent.All.reset()

        pkgInfoList.forEach { pkgInfo ->

            if (loadingPkgListJob?.isActive != true) return

            val skipDisabledApp = hideDisabledApps && !pkgInfo.applicationInfo.enabled

            if (!skipDisabledApp) {

                // skip getting stats if custom list is loaded
                val stats = customListName?.let { null }
                    ?: PackageManagerHelper.getStorageStats(this, pkgInfo)

                // update only stats if run cleaning process of custom list
                if (isCustomListClearOnly) {
                    if (PlaceholderContent.All.contains(pkgInfo)) {
                        PlaceholderContent.All.updateStats(pkgInfo, stats)
                    } else {
                        // skip getting the label of app it can take a lot of time on old phones
                        PlaceholderContent.All.add(pkgInfo, pkgInfo.packageName, locale, stats)
                    }
                } else {
                    if (PlaceholderContent.All.contains(pkgInfo)) {
                        PlaceholderContent.All.updateStats(pkgInfo, stats)
                        if (!PlaceholderContent.All.isSameLabelLocale(pkgInfo, locale)) {
                            val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                            PlaceholderContent.All.updateLabel(pkgInfo, label, locale)
                        }
                    } else {
                        val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                        PlaceholderContent.All.add(pkgInfo, label, locale, stats)
                    }
                }
            }

            progressApps += 1

            runOnUiThread {
                binding.progressBarPackageList.incrementProgressBy(1)
                binding.textProgressPackageList.text = String.format(
                    Locale.getDefault(),
                    "%d / %d", progressApps, totalApps
                )
            }
        }

        if (loadingPkgListJob?.isActive != true) return

        when (customListName) {
            null -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    PlaceholderContent.Current.update(
                        PlaceholderContent.All.getFilteredByCacheSize(
                            SharedPreferencesManager.Filter.getMinCacheSize(this))
                    )
                else
                    PlaceholderContent.Current.update(
                        PlaceholderContent.All.getSorted())
            }
            else -> {
                val checkedPkgList = SharedPreferencesManager.PackageList.get(this, customListName!!)
                PlaceholderContent.All.check(checkedPkgList)
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getSortedByLabel())
            }
        }

        if (loadingPkgListJob?.isActive != true) return

        loadingPkgListJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                runOnUiThread {
                    if (isCustomListClearOnly)
                        startCleanCache(
                            PlaceholderContent.Current.getCheckedPackageNames().toMutableList())
                    else
                        showPackageFragment()
                }
            }
        }
    }

    private fun preparePackageList(customListName: String?,
                                   pkgInfoList: ArrayList<PackageInfo>,
                                   isCustomListClearOnly: Boolean) {
        this.customListName = customListName

        hideFragmentViews()
        hideMainViews()

        updateActionBarPackageList()

        binding.textProgressPackageList.text = String.format(
            Locale.getDefault(),
            "%d / %d", 0, pkgInfoList.size
        )
        binding.progressBarPackageList.progress = 0
        binding.progressBarPackageList.max = pkgInfoList.size
        binding.layoutProgress.visibility = View.VISIBLE

        loadingPkgListJob?.cancel()
        loadingPkgListJob =
            CoroutineScope(Dispatchers.IO).launch {
                addPackageToPlaceholderContent(pkgInfoList, isCustomListClearOnly)
            }
    }

    internal fun showCustomListPackageFragment(name: String) {
        preparePackageList(name,
            PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = true,
                systemUpdated = true,
                userOnly = true,
            ),
            false
        )
    }

    private fun setSettings() {
        val intent = Intent(Constant.Intent.Settings.ACTION)

        ExtraSearchTextHelper.getTextForClearCache(this).let { list ->
            if (list.isNotEmpty())
                intent.putExtra(Constant.Intent.Settings.NAME_CLEAR_CACHE_TEXT_LIST, list)
        }

        ExtraSearchTextHelper.getTextForClearData(this).let { list ->
            if (list.isNotEmpty())
                intent.putExtra(Constant.Intent.Settings.NAME_CLEAR_DATA_TEXT_LIST, list)
        }

        ExtraSearchTextHelper.getTextForStorage(this).let { list ->
            if (list.isNotEmpty())
                intent.putExtra(Constant.Intent.Settings.NAME_STORAGE_TEXT_LIST, list)
        }

        ExtraSearchTextHelper.getTextForOk(this).let { list ->
            if (list.isNotEmpty())
                intent.putExtra(Constant.Intent.Settings.NAME_OK_TEXT_LIST, list)
        }

        intent.putExtra(
            Constant.Intent.Settings.NAME_SCENARIO,
            SharedPreferencesManager.Settings.getScenario(this))

        intent.putExtra(
            Constant.Intent.Settings.NAME_DELAY_FOR_NEXT_APP_TIMEOUT,
            SharedPreferencesManager.Settings.getDelayForNextAppTimeout(this))

        intent.putExtra(
            Constant.Intent.Settings.NAME_MAX_WAIT_APP_TIMEOUT,
            SharedPreferencesManager.Settings.getMaxWaitAppTimeout(this))

        intent.putExtra(
            Constant.Intent.Settings.NAME_MAX_WAIT_CLEAR_CACHE_BUTTON_TIMEOUT,
            SharedPreferencesManager.Settings.getMaxWaitClearCacheButtonTimeout(this))

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun checkAndShowPermissionDialogs(): Boolean {
        val hasAccessibilityPermission = PermissionChecker.checkAccessibilityPermission(this)
        if (!hasAccessibilityPermission) {
            PermissionDialogBuilder.buildAccessibilityPermissionDialog(this).show()
            return false
        }

        // Usage stats permission is allow get cache size of apps only for Android 8 and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasUsageStatsPermission = PermissionChecker.checkUsageStatsPermission(this)
            if (!hasUsageStatsPermission) {
                PermissionDialogBuilder.buildUsageStatsPermissionDialog(this).show()
                return false
            }
        }

        if (BuildConfig.DEBUG) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val hasWriteExternalStoragePermission =
                    PermissionChecker.checkWriteExternalStoragePermission(this)

                if (!hasWriteExternalStoragePermission) {
                    PermissionDialogBuilder.buildWriteExternalStoragePermissionDialog(
                        this,
                        requestPermissionLauncher
                    ).show()
                    return false
                }
            }
        }

        return PermissionChecker.checkAllRequiredPermissions(this)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) return@registerForActivityResult
        ActivityHelper.startApplicationDetailsActivity(this, this.packageName)
    }

    private val requestSaveLogFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != RESULT_OK) return@registerForActivityResult

        activityResult.data?.data?.let { uri ->
            contentResolver.openOutputStream(uri)?.let { outputStream ->
                try {
                    val inputStream = File(cacheDir.absolutePath + "/log.txt").inputStream()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        FileUtils.copy(inputStream, outputStream)
                    } else {
                        val buffer = ByteArray(8192)
                        var t: Int
                        while (inputStream.read(buffer).also { t = it } != -1)
                            outputStream.write(buffer, 0, t)
                    }
                    outputStream.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveLogFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "appcachecleaner-log.txt")
        }
        requestSaveLogFileLauncher.launch(intent)
    }

    private fun hideFragmentViews() {
        // interrupt package list preparation
        loadingPkgListJob?.cancel()

        binding.fragmentContainerView.visibility = View.GONE
        binding.layoutFab.visibility = View.GONE
        binding.layoutFabCustomList.visibility = View.GONE
        binding.layoutProgress.visibility = View.GONE
    }

    private fun showMainViews() {
        binding.layoutButton.visibility = View.VISIBLE
        updateExtraButtonsVisibility()
        updateStartStopServiceButton()
        restoreActionBar()
    }

    private fun hideMainViews() {
        binding.layoutButton.visibility = View.GONE
    }

    private fun updateActionBar(fragment: Fragment) {
        when (fragment) {
            is PackageListFragment -> updateActionBarPackageList()
            is HelpFragment -> updateActionBarTextAndHideMenu(R.string.menu_item_help)
            is SettingsFragment -> updateActionBarTextAndHideMenu(R.string.menu_item_settings)
            else -> restoreActionBar()
        }
    }

    private fun updateActionBarPackageList() {
        customListName?.let {
            updateActionBarSearch(customListName)
        } ?: updateActionBarFilter(R.string.clear_cache_btn_text)
    }

    private fun updateActionBarTextAndHideMenu(@StringRes resId: Int) {
        supportActionBar?.setTitle(resId)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        onMenuHideAll()
    }

    private fun updateActionBarFilter(@StringRes resId: Int) {
        supportActionBar?.setTitle(resId)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        onMenuShowFilter()
    }

    private fun updateActionBarSearch(title: String?) {
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        onMenuShowSearch()
    }

    private fun restoreActionBar() {
        supportActionBar?.setTitle(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        onMenuShowMain()
    }

    private fun showMenuFragment(fragment: Fragment, @StringRes title: Int) {
        hideFragmentViews()
        hideMainViews()
        binding.fragmentContainerView.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container_view,
                fragment,
                FRAGMENT_CONTAINER_VIEW_TAG
            )
            .commitNow()
        updateActionBarTextAndHideMenu(title)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showFilterDialog() {
        minCacheSizeStr ?: run {
            val minCacheSizeBytes = SharedPreferencesManager.Filter.getMinCacheSize(this)
            minCacheSizeStr =
                if (minCacheSizeBytes >= 0L)
                    DataSize.ofBytes(minCacheSizeBytes).toFormattedString(this)
                else
                    null
        }

        FilterListDialogBuilder.buildMinCacheSizeDialog(this, minCacheSizeStr) { str ->
            try {
                val dataSize = DataSize.parse(str)
                val minCacheSizeBytes = dataSize.toBytes()
                minCacheSizeStr =
                    if (minCacheSizeBytes >= 0L)
                        DataSize.ofBytes(minCacheSizeBytes).toFormattedString(this)
                    else
                        null

                supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                    ?.let { fragment ->
                        if (fragment is PackageListFragment)
                            fragment.swapAdapterFilterByCacheBytes(minCacheSizeBytes)
                    }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    getText(R.string.toast_error_filter_min_cache_size),
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }.show()
    }

    private fun updateExtraButtonsVisibility() {
        binding.btnStartStopService.visibility =
            when (SharedPreferencesManager.Extra.getShowStartStopService(this)) {
                true -> View.VISIBLE
                else -> View.GONE
            }

        binding.btnCloseApp.visibility =
            when (SharedPreferencesManager.Extra.getShowCloseApp(this)) {
                true -> View.VISIBLE
                else -> View.GONE
            }

        binding.btnCleanCustomListAppCache.visibility =
            when (SharedPreferencesManager.PackageList.getNames(this).isNotEmpty()) {
                true -> View.VISIBLE
                else -> View.GONE
            }
    }

    private fun updateStartStopServiceButton() {
        val hasPermission = PermissionChecker.checkAccessibilityPermission(this)
        val resId = when (hasPermission) {
            true -> R.string.btn_stop_accessibility_service
            else -> R.string.btn_start_accessibility_service
        }
        runOnUiThread {
            binding.btnStartStopService.setText(resId)
        }
    }

    private fun updateMainText(text: CharSequence?) {
        runOnUiThread {
            binding.textView.text = text
        }
    }

    private fun restoreUI() {
        // interrupt package list preparation if user has rotated screen
        loadingPkgListJob?.cancel()

        supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
            ?.let { frag ->
                hideFragmentViews()
                hideMainViews()
                binding.fragmentContainerView.visibility = View.VISIBLE
                when (frag) {
                    is PackageListFragment -> {
                        customListName = frag.arguments?.getString(Constant.Bundle.PackageFragment.KEY_CUSTOM_LIST_NAME)
                        when (customListName) {
                            null -> {
                                binding.layoutFab.visibility = View.VISIBLE
                                binding.layoutFabCustomList.visibility = View.GONE
                            }
                            else -> {
                                binding.layoutFab.visibility = View.GONE
                                binding.layoutFabCustomList.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                updateActionBar(frag)
                supportFragmentManager.beginTransaction()
                    .show(frag)
                    .commitNowAllowingStateLoss()
            } ?: restoreActionBar()
    }

    private fun handleOnBackPressed() {
        // always reset custom list name to avoid undefined behavior
        minCacheSizeStr = null
        customListName = null

        hideFragmentViews()
        supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
            ?.let { fragment ->
                supportFragmentManager.beginTransaction().remove(fragment).commitNow()
            }
        showMainViews()
    }

    private fun showPackageFragment() {
        binding.layoutProgress.visibility = View.GONE
        binding.fragmentContainerView.visibility = View.VISIBLE
        binding.layoutFabCustomList.visibility =
            customListName?.let { View.VISIBLE } ?: View.GONE
        binding.layoutFab.visibility =
            customListName?.let { View.GONE } ?: View.VISIBLE

        binding.fabCheckAllApps.tag = "uncheck"

        val pkgFragment = PackageListFragment.newInstance()
        Bundle().apply {
            putString(Constant.Bundle.PackageFragment.KEY_CUSTOM_LIST_NAME, customListName)
            pkgFragment.arguments = this
        }
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container_view,
                pkgFragment,
                FRAGMENT_CONTAINER_VIEW_TAG
            )
            .commitNowAllowingStateLoss()
    }

    override fun onCleanCacheFinish(interrupted: Boolean) {
        val resId: Int

        // run job to calculate cleaned cache on Android 8 and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            calculationCleanedCacheJob?.cancel()
            calculationCleanedCacheJob =
                CoroutineScope(Dispatchers.IO).launch {
                    calculateCleanedCache(interrupted)
                }
            resId = when (interrupted) {
                true -> R.string.text_clean_cache_interrupt_processing
                else -> R.string.text_clean_cache_finish_processing
            }
        } else {
            resId = when (interrupted) {
                true -> R.string.text_clean_cache_interrupt
                else -> R.string.text_clean_cache_finish
            }
        }

        val displayText = getString(resId)

        updateMainText(displayText)
        updateStartStopServiceButton()

        // return back to Main Activity, sometimes not possible press Back from Settings
        ActivityHelper.returnBackToMainActivity(this,
            this.intent.putExtra(ARG_DISPLAY_TEXT, displayText))

        if (BuildConfig.DEBUG)
            saveLogFile()

        // Automatically disable service
        if (SharedPreferencesManager.Extra.getAfterClearingCacheStopService(this)) {
            if (PermissionChecker.checkAccessibilityPermission(this))
                localBroadcastManager.disableAccessibilityService()
        }

        // Automatically close app
        if (SharedPreferencesManager.Extra.getAfterClearingCacheCloseApp(this)) {
            finish()
        }
    }

    override fun onStopAccessibilityServiceFeedback() {
        updateStartStopServiceButton()
    }

    private fun checkRequestAddTileService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return

        getSystemService(StatusBarManager::class.java)?.requestAddTileService(
            ComponentName(
                this,
                CacheCleanerTileService::class.java
            ),
            getString(R.string.tile_name),
            Icon.createWithResource(this, R.drawable.ic_baseline_icon_tile_24),
            {
                Logger.d("requestAddTileService result success")
                runOnUiThread {
                    // "requestAddTileService result success"
                }
            },
            { resultCodeFailure ->
                Logger.d("requestAddTileService failure: resultCodeFailure: $resultCodeFailure")
                val resultFailureText =
                    when (val ret = TileRequestResult.findByCode(resultCodeFailure)) {
                        TileRequestResult.TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND,
                        TileRequestResult.TILE_ADD_REQUEST_ERROR_BAD_COMPONENT,
                        TileRequestResult.TILE_ADD_REQUEST_ERROR_MISMATCHED_PACKAGE,
                        TileRequestResult.TILE_ADD_REQUEST_ERROR_NOT_CURRENT_USER,
                        TileRequestResult.TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE,
                        TileRequestResult.TILE_ADD_REQUEST_ERROR_REQUEST_IN_PROGRESS,
                        TileRequestResult.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
                        TileRequestResult.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
                        TileRequestResult.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> {
                            ret.name
                        }
                        null -> {
                            "unknown resultCodeFailure: $resultCodeFailure"
                        }
                    }
                runOnUiThread {
                    // resultFailureText
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateCleanedCache(interrupted: Boolean) {
        val cleanCacheBytes =
            PlaceholderContent.Current.getChecked().sumOf {
                PackageManagerHelper.getCacheSizeDiff(
                    it.stats,
                    PackageManagerHelper.getStorageStats(this, it.pkgInfo)
                )
            }

        calculationCleanedCacheJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                val resId = when (interrupted) {
                    true -> R.string.text_clean_cache_interrupt_display_size
                    else -> R.string.text_clean_cache_finish_display_size
                }

                val displayText = getString(resId,
                    DataSize.ofBytes(cleanCacheBytes).toFormattedString(this))

                updateMainText(displayText)
            }
        }
    }
}