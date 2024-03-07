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
import com.github.bmx666.appcachecleaner.const.Constant.Bundle.AppCacheCleanerActivity.Companion.KEY_SKIP_FIRST_RUN
import com.github.bmx666.appcachecleaner.databinding.ActivityMainBinding
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.service.CacheCleanerTileService
import com.github.bmx666.appcachecleaner.ui.dialog.AlertDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.CustomListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.FilterListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.IgnoreAppDialogBuilder
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
import java.util.HashSet
import java.util.Locale

class AppCacheCleanerActivity : AppCompatActivity(), IIntentActivityCallback {

    companion object {
        const val ARG_DISPLAY_TEXT = "display-text"
        const val FRAGMENT_CONTAINER_VIEW_TAG = "fragment-container-view-tag"
    }

    private lateinit var binding: ActivityMainBinding
    private var minCacheSizeStr: String? = null

    private var customListName: String? = null
    private var currentPkgListAction = Constant.PackageListAction.DEFAULT

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

            preparePackageList(
                PackageManagerHelper.getInstalledApps(
                    context = this,
                    systemNotUpdated = false,
                    systemUpdated = true,
                    userOnly = true,
                ),
                pkgListAction = Constant.PackageListAction.DEFAULT,
            )
        }

        binding.btnCleanSystemAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            preparePackageList(
                PackageManagerHelper.getInstalledApps(
                    context = this,
                    systemNotUpdated = true,
                    systemUpdated = false,
                    userOnly = false,
                ),
                pkgListAction = Constant.PackageListAction.DEFAULT,
            )
        }

        binding.btnCleanAllAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            preparePackageList(
                PackageManagerHelper.getInstalledApps(
                    context = this,
                    systemNotUpdated = true,
                    systemUpdated = true,
                    userOnly = true,
                ),
                pkgListAction = Constant.PackageListAction.DEFAULT,
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

        binding.fabListOfIgnoredAppsOk.setOnClickListener {
            val checkedPkgList = PlaceholderContent.Current.getCheckedPackageNames().toSet()
            SharedPreferencesManager.Filter.setListOfIgnoredApps(this, checkedPkgList)
            handleOnBackPressed()
        }

        binding.fabListOfIgnoredAppsCancel.setOnClickListener {
            handleOnBackPressed()
        }

        binding.btnCleanCustomListAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            CustomListDialogBuilder.buildCleanCacheDialog(this) { name ->
                name ?: return@buildCleanCacheDialog

                customListName = name

                val pkgList = SharedPreferencesManager.PackageList.get(this, name)
                preparePackageList(
                    PackageManagerHelper.getCustomInstalledApps(
                        context = this,
                        pkgList = pkgList
                    ),
                    pkgListAction = Constant.PackageListAction.CUSTOM_CLEAN,
                    checkedPkgList = pkgList,
                )
            }.show()
        }

        if (calculationCleanedCacheJob?.isActive != true)
            updateMainText(intent.getCharSequenceExtra(ARG_DISPLAY_TEXT))

        val skipFirstRun = savedInstanceState?.getBoolean(KEY_SKIP_FIRST_RUN) == true
        if (!skipFirstRun) {
            checkRequestAddTileService()

            // Show bugs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (SharedPreferencesManager.BugWarning.showBug322519674(this)) {
                    AlertDialogBuilder(this)
                        .setTitle(R.string.title_bug_322519674)
                        .setMessage(R.string.message_bug_322519674)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            // SharedPreferencesManager.BugWarning.hideBug322519674(this)
                        }
                        .create()
                        .show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_SKIP_FIRST_RUN, true)
        super.onSaveInstanceState(outState)
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
                                               pkgListAction: Constant.PackageListAction,
                                               checkedPkgList: Set<String>) {
        val locale = LocaleHelper.getCurrentLocale(this)
        val hideDisabledApps = SharedPreferencesManager.Filter.getHideDisabledApps(this)
        val hideIgnoredApps = SharedPreferencesManager.Filter.getHideIgnoredApps(this)
        val listOfIgnoreApps = SharedPreferencesManager.Filter.getListOfIgnoredApps(this)

        var progressApps = 0
        val totalApps = pkgInfoList.size

        val skipPkgList = HashSet<String>()

        PlaceholderContent.All.reset()

        pkgInfoList.forEach { pkgInfo ->

            if (loadingPkgListJob?.isActive != true) return

            val isDisabledAndHidden =
                hideDisabledApps
                && !pkgInfo.applicationInfo.enabled

            val isIgnoredAndHidden =
                hideIgnoredApps
                && listOfIgnoreApps.contains(pkgInfo.packageName)

            if (isDisabledAndHidden || isIgnoredAndHidden)
                skipPkgList.add(pkgInfo.packageName)

            when (pkgListAction) {
                Constant.PackageListAction.CUSTOM_CLEAN -> {
                    if (PlaceholderContent.All.contains(pkgInfo)) {
                        PlaceholderContent.All.updateStats(pkgInfo, null)
                    } else {
                        // skip getting the label of app it can take a lot of time on old phones
                        PlaceholderContent.All.add(pkgInfo, pkgInfo.packageName, locale, null)
                    }
                }
                else -> {
                    // skip getting cache size for custom list and list of ignored apps
                    val stats =
                        when (pkgListAction) {
                            Constant.PackageListAction.DEFAULT ->
                                PackageManagerHelper.getStorageStats(this, pkgInfo)
                            else ->
                                null
                        }

                    if (PlaceholderContent.All.contains(pkgInfo)) {
                        PlaceholderContent.All.updateStats(pkgInfo, stats)
                        if (PlaceholderContent.All.isLabelAsPackageName(pkgInfo) ||
                            !PlaceholderContent.All.isSameLabelLocale(pkgInfo, locale)) {
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

        // hide ignored and disabled packages for default action only
        if (pkgListAction == Constant.PackageListAction.DEFAULT)
            PlaceholderContent.All.ignore(skipPkgList)

        when (pkgListAction) {
            Constant.PackageListAction.DEFAULT -> {
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
                PlaceholderContent.All.check(checkedPkgList)
                PlaceholderContent.Current.update(
                    PlaceholderContent.All.getSortedByLabel())
            }
        }

        if (loadingPkgListJob?.isActive != true) return

        loadingPkgListJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                runOnUiThread {
                    when (pkgListAction) {
                        Constant.PackageListAction.CUSTOM_CLEAN ->
                            startCleanCache(
                                PlaceholderContent.Current.getCheckedPackageNames().toMutableList())
                        else -> {
                            showPackageFragment(pkgListAction)
                            onMenuShowFilter()
                        }
                    }
                }
            }
        }
    }

    private fun preparePackageList(pkgInfoList: ArrayList<PackageInfo>,
                                   pkgListAction: Constant.PackageListAction,
                                   checkedPkgList: Set<String> = HashSet<String>()) {
        hideFragmentViews()
        hideMainViews()

        // save current package list action
        currentPkgListAction = pkgListAction
        updateActionBarPackageList(pkgListAction)
        onMenuHideAll()

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
                addPackageToPlaceholderContent(
                    pkgInfoList,
                    pkgListAction,
                    checkedPkgList)
            }
    }

    internal fun showCustomListPackageFragment(name: String) {
        customListName = name
        preparePackageList(
            PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = true,
                systemUpdated = true,
                userOnly = true,
            ),
            pkgListAction = Constant.PackageListAction.CUSTOM_ADD_EDIT,
            checkedPkgList = SharedPreferencesManager.PackageList.get(this, name),
        )
    }

    internal fun showIgnoredListPackageFragment() {
        preparePackageList(
            PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = true,
                systemUpdated = true,
                userOnly = true,
            ),
            pkgListAction = Constant.PackageListAction.IGNORED_APPS_EDIT,
            checkedPkgList = SharedPreferencesManager.Filter.getListOfIgnoredApps(this)
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
        binding.layoutFabListOfIgnoredApps.visibility = View.GONE
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

    private fun updateActionBarPackageList(pkgListAction: Constant.PackageListAction?) {
        when (pkgListAction) {
            Constant.PackageListAction.DEFAULT ->
                updateActionBarFilter(R.string.clear_cache_btn_text)
            Constant.PackageListAction.CUSTOM_ADD_EDIT ->
                updateActionBarSearch(customListName)
            Constant.PackageListAction.CUSTOM_CLEAN ->
                return
            Constant.PackageListAction.IGNORED_APPS_EDIT ->
                updateActionBarSearch(R.string.text_list_ignored_apps)
            // in case of undefined behavior
            null ->
                updateActionBarFilter(R.string.clear_cache_btn_text)
        }
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

    private fun updateActionBarSearch(@StringRes resId: Int) {
        supportActionBar?.setTitle(resId)
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
                        val pkgListAction =
                            try {
                                frag.arguments?.getString(
                                    Constant.Bundle.PackageFragment.KEY_PACKAGE_LIST_ACTION)
                                    ?.let { enumStr ->
                                        Constant.PackageListAction.valueOf(enumStr)
                                    }
                            } catch (e: Exception) {
                                null
                            }

                        // restore current package list action for activity
                        pkgListAction?.let {
                            currentPkgListAction = it
                        }

                        // restore current custom list name for activity
                        customListName =
                            try {
                                frag.arguments?.getString(
                                    Constant.Bundle.PackageFragment.KEY_CUSTOM_LIST_NAME)
                            } catch (e: Exception) {
                                null
                            }

                        binding.layoutFab.visibility = View.GONE
                        binding.layoutFabCustomList.visibility = View.GONE
                        binding.layoutFabListOfIgnoredApps.visibility = View.GONE

                        when (pkgListAction) {
                            Constant.PackageListAction.DEFAULT ->
                                binding.layoutFab.visibility = View.VISIBLE
                            Constant.PackageListAction.CUSTOM_ADD_EDIT ->
                                binding.layoutFabCustomList.visibility = View.VISIBLE
                            Constant.PackageListAction.IGNORED_APPS_EDIT ->
                                binding.layoutFabListOfIgnoredApps.visibility = View.VISIBLE
                            else -> {}
                        }

                        updateActionBarPackageList(pkgListAction)
                    }

                    is HelpFragment -> updateActionBarTextAndHideMenu(R.string.menu_item_help)
                    is SettingsFragment -> updateActionBarTextAndHideMenu(R.string.menu_item_settings)
                    else -> restoreActionBar()
                }
                supportFragmentManager.beginTransaction()
                    .show(frag)
                    .commitNowAllowingStateLoss()
            } ?: restoreActionBar()
    }

    private fun handleOnBackPressed() {
        minCacheSizeStr = null
        // always reset to default package list type to avoid undefined behavior
        currentPkgListAction = Constant.PackageListAction.DEFAULT

        hideFragmentViews()
        supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
            ?.let { fragment ->
                supportFragmentManager.beginTransaction().remove(fragment).commitNow()
            }
        showMainViews()
    }

    private fun showPackageFragment(pkgListAction: Constant.PackageListAction) {
        binding.layoutProgress.visibility = View.GONE
        binding.fragmentContainerView.visibility = View.VISIBLE
        binding.layoutFab.visibility = View.GONE
        binding.layoutFabCustomList.visibility = View.GONE
        binding.layoutFabListOfIgnoredApps.visibility = View.GONE

        when (pkgListAction) {
            Constant.PackageListAction.DEFAULT ->
                binding.layoutFab.visibility = View.VISIBLE
            Constant.PackageListAction.CUSTOM_ADD_EDIT ->
                binding.layoutFabCustomList.visibility = View.VISIBLE
            Constant.PackageListAction.CUSTOM_CLEAN ->
                { /* not valid */ }
            Constant.PackageListAction.IGNORED_APPS_EDIT ->
                binding.layoutFabListOfIgnoredApps.visibility = View.VISIBLE
        }

        binding.fabCheckAllApps.tag = "uncheck"

        val pkgFragment = PackageListFragment.newInstance()
        Bundle().apply {
            putString(Constant.Bundle.PackageFragment.KEY_PACKAGE_LIST_ACTION,
                pkgListAction.name)
            putString(Constant.Bundle.PackageFragment.KEY_CUSTOM_LIST_NAME,
                customListName)
            putBoolean(Constant.Bundle.PackageFragment.KEY_HIDE_STATS,
                pkgListAction != Constant.PackageListAction.DEFAULT)
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

    override fun onCleanCacheFinish(interrupted: Boolean,
                                    interruptedByUser: Boolean,
                                    pkgName: String?) {
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
            return
        }

        // Show dialog to ignore app
        if (SharedPreferencesManager.Filter.getShowDialogToIgnoreApp(this)) {
            pkgName?.takeIf { interrupted }?.let {
                val label = PlaceholderContent.Current.find(pkgName)?.label
                IgnoreAppDialogBuilder.buildIgnoreAppDialog(
                    context = this,
                    pkgName = pkgName,
                    fullPkgName = when {
                        label.isNullOrEmpty() -> "$pkgName"
                        else -> "$label ($pkgName)"
                    }
                ).show()
            }
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