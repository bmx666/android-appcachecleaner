package com.github.bmx666.appcachecleaner.ui.activity

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.FileUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.Composable
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.github.bmx666.appcachecleaner.ui.theme.AppTheme
import com.github.bmx666.appcachecleaner.ui.compose.ComposeHelp
import com.github.bmx666.appcachecleaner.ui.compose.FirstBootScreen
import com.github.bmx666.appcachecleaner.ui.compose.HelpScreen
import com.github.bmx666.appcachecleaner.ui.compose.HomeScreen
import com.github.bmx666.appcachecleaner.ui.compose.SettingsScreen
import com.github.bmx666.appcachecleaner.ui.dialog.CustomListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.FilterListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.IgnoreAppDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.PermissionDialogBuilder
import com.github.bmx666.appcachecleaner.ui.fragment.PackageListFragment
import com.github.bmx666.appcachecleaner.ui.fragment.SettingsFragment
import com.github.bmx666.appcachecleaner.ui.theme.AppTheme
import com.github.bmx666.appcachecleaner.util.ActivityHelper
import com.github.bmx666.appcachecleaner.util.ExtraSearchTextHelper
import com.github.bmx666.appcachecleaner.util.IIntentActivityCallback
import com.github.bmx666.appcachecleaner.util.LocalBroadcastManagerActivityHelper
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.PermissionChecker
import com.github.bmx666.appcachecleaner.util.TileRequestResult
import com.github.bmx666.appcachecleaner.util.toFormattedString
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private var currentPkgListAction = Constant.PackageListAction.DEFAULT

    //private lateinit var onMenuHideAll: () -> Unit
    //private lateinit var onMenuShowMain: () -> Unit
    //private lateinit var onMenuShowFilter: () -> Unit
    //private lateinit var onMenuShowSearch: () -> Unit

    private lateinit var localBroadcastManager: LocalBroadcastManagerActivityHelper

    private lateinit var snackbar: Snackbar

    private var calculationCleanedCacheJob: Job? = null
    private var loadingPkgListJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localBroadcastManager = LocalBroadcastManagerActivityHelper(this, this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        snackbar = Snackbar.make(binding.root,
            getString(R.string.snackbar_processing),
            Snackbar.LENGTH_INDEFINITE)

        setSupportActionBar(binding.toolbar)

        binding.overlayView.setShowOverlayCallback {
            runOnUiThread {
                binding.overlayView.visibility = View.VISIBLE
                binding.appBarLayout.isEnabled = false
                snackbar.show()
            }
        }

        binding.overlayView.setHideOverlayCallback {
            runOnUiThread {
                snackbar.dismiss()
                binding.appBarLayout.isEnabled = true
                binding.overlayView.visibility = View.GONE
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    this@AppCacheCleanerActivity.handleOnBackPressed()
                }
            }
        )

        binding.btnCleanUserAppCache.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    if (checkAndShowPermissionDialogs())
                        PackageManagerHelper.getInstalledApps(
                            context = this,
                            systemNotUpdated = false,
                            systemUpdated = true,
                            userOnly = true,
                        )
                    else
                        null
                },
                postUiCallback = { pkgInfoList ->
                    pkgInfoList ?: return@addOverlayJob
                    preparePackageList(
                        pkgInfoList,
                        Constant.PackageListAction.DEFAULT,
                    )
                }
            )
        }

        binding.btnCleanSystemAppCache.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    if (checkAndShowPermissionDialogs())
                        PackageManagerHelper.getInstalledApps(
                            context = this,
                            systemNotUpdated = true,
                            systemUpdated = false,
                            userOnly = false,
                        )
                    else
                        null
                },
                postUiCallback = { pkgInfoList ->
                    pkgInfoList ?: return@addOverlayJob
                    preparePackageList(
                        pkgInfoList,
                        Constant.PackageListAction.DEFAULT,
                    )
                }
            )
        }

        binding.btnCleanAllAppCache.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    if (checkAndShowPermissionDialogs())
                        PackageManagerHelper.getInstalledApps(
                            context = this,
                            systemNotUpdated = true,
                            systemUpdated = true,
                            userOnly = true,
                        )
                    else
                        null
                },
                postUiCallback = { pkgInfoList ->
                    pkgInfoList ?: return@addOverlayJob
                    preparePackageList(
                        pkgInfoList,
                        Constant.PackageListAction.DEFAULT,
                    )
                }
            )
        }

        binding.btnStartStopService.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    PermissionChecker.checkAccessibilityPermission(this)
                },
                postUiCallback = { hasAccessibilityPermission ->
                    if (hasAccessibilityPermission)
                        localBroadcastManager.disableAccessibilityService()
                    else
                        PermissionDialogBuilder.buildAccessibilityPermissionDialog(this).show()
                }
            )
        }

        binding.btnCloseApp.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    PermissionChecker.checkAccessibilityPermission(this)
                },
                postUiCallback = { hasAccessibilityPermission ->
                    if (hasAccessibilityPermission)
                        localBroadcastManager.disableAccessibilityService()
                    finish()
                }
            )
        }

        binding.fabCleanCache.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    PlaceholderContent.Current.getCheckedPackageNames().toMutableList()
                },
                postUiCallback = { pkgList ->
                    startCleanCache(pkgList)
                }
            )
        }

        binding.fabCheckAllApps.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    val state =
                        if (PlaceholderContent.Current.isAllVisibleChecked())
                            "uncheck"
                        else if (PlaceholderContent.Current.isAllVisibleUnchecked())
                            "check"
                        else
                            binding.fabCheckAllApps.tag

                    when (state) {
                        "uncheck" -> {
                            PlaceholderContent.All.uncheckVisible()
                            binding.fabCheckAllApps.tag = "check"
                        }
                        "check" -> {
                            PlaceholderContent.All.checkVisible()
                            binding.fabCheckAllApps.tag = "uncheck"
                        }
                    }
                    state
                },
                postUiCallback = { state ->
                    when (state) {
                        "uncheck" ->
                            binding.fabCheckAllApps.contentDescription =
                                getString(R.string.description_apps_all_check)
                        "check" ->
                            binding.fabCheckAllApps.contentDescription =
                                getString(R.string.description_apps_all_uncheck)
                    }
                    supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                        ?.let { fragment ->
                            if (fragment is PackageListFragment)
                                fragment.refreshAdapter()
                        }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        showTotalCacheSizeOfCheckedPackages()
                }
            )
        }

        binding.fabCustomListOk.setOnClickListener {
            val currentListName = customListName

            addOverlayJob(
                suspendCallback = {
                    val checkedPkgList = PlaceholderContent.Current.getCheckedPackageNames().toSet()
                    if (checkedPkgList.isEmpty()) {
                        showToast(R.string.toast_custom_list_add_list_empty)
                    } else {
                        currentListName?.let { name ->
                            SharedPreferencesManager.PackageList.save(
                                this, name, checkedPkgList)
                            showToast(R.string.toast_custom_list_has_been_saved, name)
                        }
                    }
                },
                postUiCallback = {
                    handleOnBackPressed()
                }
            )
        }

        binding.fabCustomListCancel.setOnClickListener {
            handleOnBackPressed()
        }

        binding.fabListOfIgnoredAppsOk.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    val checkedPkgList = PlaceholderContent.Current.getCheckedPackageNames().toSet()
                    SharedPreferencesManager.Filter.setListOfIgnoredApps(
                        this, checkedPkgList)
                },
                postUiCallback = {
                    handleOnBackPressed()
                }
            )
        }

        binding.fabListOfIgnoredAppsCancel.setOnClickListener {
            handleOnBackPressed()
        }

        binding.btnCleanCustomListAppCache.setOnClickListener {
            addOverlayJob(
                suspendCallback = {
                    if (checkAndShowPermissionDialogs())
                        SharedPreferencesManager.PackageList.getNames(this).sorted()
                    else
                        null
                },
                postUiCallback = { pkgListNames ->
                    pkgListNames ?: return@addOverlayJob
                    showCustomListDialog(pkgListNames)
                }
            )
        }

        if (calculationCleanedCacheJob?.isActive != true)
            updateMainText(intent.getCharSequenceExtra(ARG_DISPLAY_TEXT))

        val startDestination: String

        if (SharedPreferencesManager.FirstBoot.showFirstBootConfirmation(this)) {
            startDestination = Constant.Navigation.FIRST_BOOT.name
        } else {
            startDestination = Constant.Navigation.HOME.name
            checkRequestAddTileService()
            // Show bugs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                addOverlayJob(
                    suspendCallback = {
                        SharedPreferencesManager.BugWarning.showBug322519674(this)
                    },
                    postUiCallback = { showBug ->
                        if (!showBug)
                            return@addOverlayJob

                        AlertDialogBuilder(this)
                            .setTitle(R.string.title_bug_322519674)
                            .setMessage(R.string.message_bug_322519674)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                addOverlayJob(suspendCallback = {
                                    // SharedPreferencesManager.BugWarning.hideBug322519674(this)
                                })
                            }
                            .create()
                            .show()
                    }
                )
            }
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    composable(Constant.Navigation.FIRST_BOOT.name) {
                        FirstBootScreen(
                            activity = this@AppCacheCleanerActivity,
                            navController = navController,
                        )
                    }
                    composable(Constant.Navigation.HOME.name) {
                        HomeScreen(
                            navController = navController,
                        )
                    }
                    composable(Constant.Navigation.HELP.name) {
                        HelpScreen(
                            navController = navController,
                        )
                    }
                    composable(Constant.Navigation.SETTINGS.name) {
                        SettingsScreen(
                            navController = navController,
                        )
                    }
                    //composable("package_list") { PackageListScreen(navController) }
                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    //    showFilterDialog()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_SKIP_FIRST_RUN, true)
        super.onSaveInstanceState(outState)
    }

    /*
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
                addOverlayJob(suspendCallback = {
                    // unset ignore flag for all packages in the current list
                    PlaceholderContent.All.unignore(
                        PlaceholderContent.Current.getPackageNames().toSet())
                    PlaceholderContent.Current.update(
                        PlaceholderContent.All.getSortedByLabel())
                })
                false
            }
        }

        restoreUI()
        return true
    }
    */

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

    @UiContext
    @UiThread
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

    private suspend fun addPackageToPlaceholderContent(pkgInfoList: ArrayList<PackageInfo>,
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

            // skip getting cache size when add/edit custom list or for the list of ignored apps
            val requestStats = pkgListAction !in setOf(
                Constant.PackageListAction.CUSTOM_ADD_EDIT,
                Constant.PackageListAction.IGNORED_APPS_EDIT
            )

            // skip getting the label of app, it can take a lot of time on old phones
            val requestLabel = pkgListAction !in setOf(
                Constant.PackageListAction.CUSTOM_CLEAN
            )

            val stats =
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && requestStats)
                    PackageManagerHelper.getStorageStats(this, pkgInfo)
                else
                    null

            if (PlaceholderContent.All.contains(pkgInfo)) {
                PlaceholderContent.All.updateStats(pkgInfo, stats)
                if (requestLabel) {
                    if (PlaceholderContent.All.isLabelAsPackageName(pkgInfo) ||
                        !PlaceholderContent.All.isSameLabelLocale(pkgInfo, locale)) {
                        val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                        PlaceholderContent.All.updateLabel(pkgInfo, label, locale)
                    }
                }
            } else {
                val label =
                    if (requestLabel)
                        PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                    else
                        pkgInfo.packageName

                PlaceholderContent.All.add(pkgInfo, label, locale, stats)
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
                when (pkgListAction) {
                    Constant.PackageListAction.CUSTOM_CLEAN -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            val pkgList = PlaceholderContent.Current.getCheckedPackageNames().toMutableList()
                            runOnUiThread {
                                startCleanCache(pkgList)
                            }
                        }
                    }
                    else ->
                        runOnUiThread {
                            showPackageFragment(pkgListAction)
                        }
                }
            }
        }
    }

    @UiThread
    @UiContext
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

        addOverlayJob(
            suspendCallback = {
                val pkgInfoList =
                    PackageManagerHelper.getInstalledApps(
                        context = this,
                        systemNotUpdated = true,
                        systemUpdated = true,
                        userOnly = true,
                    )
                val checkedPkgList =
                    SharedPreferencesManager.PackageList.get(this, name)

                Pair(pkgInfoList, checkedPkgList)
            },
            postUiCallback = { (pkgInfoList, checkedPkgList) ->
                preparePackageList(
                    pkgInfoList = pkgInfoList,
                    pkgListAction = Constant.PackageListAction.CUSTOM_ADD_EDIT,
                    checkedPkgList = checkedPkgList,
                )
            }
        )
    }

    internal fun showIgnoredListPackageFragment() {
        addOverlayJob(
            suspendCallback = {
                val pkgInfoList =
                    PackageManagerHelper.getInstalledApps(
                        context = this,
                        systemNotUpdated = true,
                        systemUpdated = true,
                        userOnly = true,
                    )
                val checkedPkgList =
                    SharedPreferencesManager.Filter.getListOfIgnoredApps(this)

                Pair(pkgInfoList, checkedPkgList)
            },
            postUiCallback = { (pkgInfoList, checkedPkgList) ->
                preparePackageList(
                    pkgInfoList = pkgInfoList,
                    pkgListAction = Constant.PackageListAction.IGNORED_APPS_EDIT,
                    checkedPkgList = checkedPkgList
                )
            }
        )
    }

    private fun setSettings() {
        addOverlayJob(
            suspendCallback = {
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
        )
    }

    private suspend fun checkAndShowPermissionDialogs(): Boolean {
        val hasAccessibilityPermission = PermissionChecker.checkAccessibilityPermission(this)
        if (!hasAccessibilityPermission) {
            runOnUiThread {
                PermissionDialogBuilder.buildAccessibilityPermissionDialog(this).show()
            }
            return false
        }

        // Usage stats permission is allow get cache size of apps only for Android 8 and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasUsageStatsPermission = PermissionChecker.checkUsageStatsPermission(this)
            if (!hasUsageStatsPermission) {
                runOnUiThread {
                    PermissionDialogBuilder.buildUsageStatsPermissionDialog(this).show()
                }
                return false
            }
        }

        if (BuildConfig.DEBUG) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val hasWriteExternalStoragePermission =
                    PermissionChecker.checkWriteExternalStoragePermission(this)

                if (!hasWriteExternalStoragePermission) {
                    runOnUiThread {
                        PermissionDialogBuilder.buildWriteExternalStoragePermissionDialog(
                            this,
                            requestPermissionLauncher
                        ).show()
                    }
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

    @UiContext
    @UiThread
    private fun hideFragmentViews() {
        // interrupt package list preparation
        loadingPkgListJob?.cancel()

        binding.composeView.visibility = View.GONE
        binding.fragmentContainerView.visibility = View.GONE
        binding.layoutFab.visibility = View.GONE
        binding.layoutFabCustomList.visibility = View.GONE
        binding.layoutFabListOfIgnoredApps.visibility = View.GONE
        binding.layoutProgress.visibility = View.GONE
    }

    @UiContext
    @UiThread
    private fun showMainViews() {
        binding.layoutButton.visibility = View.VISIBLE
        updateExtraButtonsVisibility()
        updateStartStopServiceButton()
        restoreActionBar()
    }

    @UiContext
    @UiThread
    private fun hideMainViews() {
        binding.layoutButton.visibility = View.GONE
    }

    @UiContext
    @UiThread
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

    @UiContext
    @UiThread
    private fun updateActionBarTextAndHideMenu(@StringRes resId: Int) {
        //supportActionBar?.setTitle(resId)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //onMenuHideAll()
    }

    @UiContext
    @UiThread
    private fun updateActionBarFilter(@StringRes resId: Int) {
        //supportActionBar?.setTitle(resId)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //onMenuShowFilter()
    }

    @UiContext
    @UiThread
    private fun updateActionBarSearch(title: String?) {
        //supportActionBar?.title = title
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //onMenuShowSearch()
    }

    @UiContext
    @UiThread
    private fun updateActionBarSearch(@StringRes resId: Int) {
        //supportActionBar?.setTitle(resId)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //onMenuShowSearch()
    }

    @UiContext
    @UiThread
    private fun restoreActionBar() {
        //supportActionBar?.show()
        //supportActionBar?.setTitle(R.string.app_name)
        //supportActionBar?.setDisplayHomeAsUpEnabled(false)
        //onMenuShowMain()
    }

    @UiContext
    @UiThread
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
        addOverlayJob(
            suspendCallback = {
                val minCacheSizeBytes = SharedPreferencesManager.Filter.getMinCacheSize(this)
                if (minCacheSizeBytes >= 0L)
                    DataSize.ofBytes(minCacheSizeBytes).toFormattedString(this)
                else
                    null
            },
            postUiCallback = { minCacheSizeStr ->
                FilterListDialogBuilder.buildMinCacheSizeDialog(this, minCacheSizeStr) { str ->
                    try {
                        val dataSize = DataSize.parse(str)
                        val minCacheSizeBytes = dataSize.toBytes()
                        supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                            ?.let { fragment ->
                                if (fragment is PackageListFragment)
                                    fragment.swapAdapterFilterByCacheBytes(minCacheSizeBytes)
                            }
                    } catch (e: Exception) {
                        showToast(R.string.toast_error_filter_min_cache_size)
                    }
                }.show()
            }
        )
    }

    private fun updateExtraButtonsVisibility() {
        addOverlayJob(
            suspendCallback = {
                SharedPreferencesManager.Extra.getShowStartStopService(this)
            },
            postUiCallback = { showStartStopService ->
                binding.btnStartStopService.visibility =
                    when (showStartStopService) {
                        true -> View.VISIBLE
                        else -> View.GONE
                    }
            }
        )

        addOverlayJob(
            suspendCallback = {
                SharedPreferencesManager.Extra.getShowCloseApp(this)
            },
            postUiCallback = { closeApp ->
                binding.btnCloseApp.visibility =
                    when (closeApp) {
                        true -> View.VISIBLE
                        else -> View.GONE
                    }
            }
        )

        addOverlayJob(
            suspendCallback = {
                SharedPreferencesManager.PackageList.getNames(this).isNotEmpty()
            },
            postUiCallback = { hasCustomList ->
                binding.btnCleanCustomListAppCache.visibility =
                    when (hasCustomList) {
                        true -> View.VISIBLE
                        else -> View.GONE
                    }
            }
        )
    }

    private fun updateStartStopServiceButton() {
        addOverlayJob(
            suspendCallback = {
                when (PermissionChecker.checkAccessibilityPermission(this)) {
                    true -> R.string.btn_stop_accessibility_service
                    else -> R.string.btn_start_accessibility_service
                }
            },
            postUiCallback = { resId ->
                binding.btnStartStopService.setText(resId)
            }
        )
    }

    private fun updateMainText(text: CharSequence?) {
        runOnUiThread {
            binding.textView.text = text
        }
    }

    @UiContext
    @UiThread
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

                    is SettingsFragment -> updateActionBarTextAndHideMenu(R.string.menu_item_settings)
                    else -> restoreActionBar()
                }
                supportFragmentManager.beginTransaction()
                    .show(frag)
                    .commitNowAllowingStateLoss()
            } ?: restoreActionBar()
    }

    @UiContext
    @UiThread
    private fun handleOnBackPressed() {
        // always reset to default package list type to avoid undefined behavior
        currentPkgListAction = Constant.PackageListAction.DEFAULT

        /*
        hideFragmentViews()
        supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
            ?.let { fragment ->
                supportFragmentManager.beginTransaction().remove(fragment).commitNow()
            }
        showMainViews()
        */
    }

    @UiContext
    @UiThread
    private fun showPackageFragment(pkgListAction: Constant.PackageListAction) {
        binding.layoutProgress.visibility = View.GONE
        binding.fragmentContainerView.visibility = View.VISIBLE
        binding.layoutFab.visibility = View.GONE
        binding.layoutFabCustomList.visibility = View.GONE
        binding.layoutFabListOfIgnoredApps.visibility = View.GONE

        when (pkgListAction) {
            Constant.PackageListAction.DEFAULT -> {
                binding.layoutFab.visibility = View.VISIBLE
                onMenuShowFilter()
            }
            Constant.PackageListAction.CUSTOM_ADD_EDIT -> {
                binding.layoutFabCustomList.visibility = View.VISIBLE
                onMenuShowSearch()
            }
            Constant.PackageListAction.CUSTOM_CLEAN ->
                { /* not valid */ }
            Constant.PackageListAction.IGNORED_APPS_EDIT -> {
                binding.layoutFabListOfIgnoredApps.visibility = View.VISIBLE
                onMenuShowSearch()
            }
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

    @UiContext
    @UiThread
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

        // return back to Main Activity, sometimes not possible press Back from Settings
        ActivityHelper.returnBackToMainActivity(this,
            this.intent.putExtra(ARG_DISPLAY_TEXT, displayText))

        if (BuildConfig.DEBUG)
            saveLogFile()

        updateStartStopServiceButton()

        addOverlayJob(
            suspendCallback = {
                // Automatically disable service
                if (SharedPreferencesManager.Extra.getAfterClearingCacheStopService(this)) {
                    if (PermissionChecker.checkAccessibilityPermission(this))
                        localBroadcastManager.disableAccessibilityService()
                }

                // Automatically close app
                if (SharedPreferencesManager.Extra.getAfterClearingCacheCloseApp(this)) {
                    finish()
                    return@addOverlayJob
                }

                showDialogToIgnoreApp(pkgName, interrupted)
            }
        )
    }

    override fun onStopAccessibilityServiceFeedback() {
        updateStartStopServiceButton()
    }

    private fun showDialogToIgnoreApp(pkgName: String?,
                                      interrupted: Boolean) {
        addOverlayJob(
            suspendCallback = {
                if (!interrupted)
                    Pair(null, null)
                else if (!SharedPreferencesManager.Filter.getShowDialogToIgnoreApp(this))
                    Pair(null, null)
                else if (pkgName.isNullOrEmpty())
                    Pair(null, null)
                else
                    Pair(pkgName, PlaceholderContent.Current.find(pkgName)?.label)
            },
            postUiCallback = { (pkgName, label) ->
                pkgName ?: return@addOverlayJob
                IgnoreAppDialogBuilder.buildIgnoreAppDialog(
                    context = this,
                    pkgName = pkgName,
                    fullPkgName = when {
                        label.isNullOrEmpty() -> "$pkgName"
                        else -> "$label ($pkgName)"
                    }
                ).show()
            }
        )
    }

    @UiContext
    @UiThread
    private fun showCustomListDialog(pkgListNames: List<String>) {
        CustomListDialogBuilder.buildCleanCacheDialog(this, pkgListNames) { name ->
            name ?: return@buildCleanCacheDialog

            customListName = name

            addOverlayJob(
                suspendCallback = {
                    val pkgList = SharedPreferencesManager.PackageList.get(this, name)
                    val pkgInfoList =
                        PackageManagerHelper.getCustomInstalledApps(
                            context = this,
                            pkgList = pkgList
                        )
                    Pair(pkgList, pkgInfoList)
                },
                postUiCallback = { (pkgList, pkgInfoList) ->
                    preparePackageList(
                        pkgInfoList = pkgInfoList,
                        pkgListAction = Constant.PackageListAction.CUSTOM_CLEAN,
                        checkedPkgList = pkgList,
                    )
                }
            )
        }.show()
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
    private suspend fun calculateCleanedCache(interrupted: Boolean) {
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

                val sizeStr = runBlocking { DataSize.ofBytes(cleanCacheBytes).toFormattedString(this@AppCacheCleanerActivity) }
                val displayText = getString(resId, sizeStr)

                updateMainText(displayText)
            }
        }
    }

    private fun showToast(@StringRes resId: Int, vararg formatArgs: Any?) {
        runOnUiThread {
            Toast.makeText(this, getString(resId, *formatArgs), Toast.LENGTH_SHORT).show()
        }
    }

    internal fun <T> addOverlayJob(
        suspendCallback: suspend () -> T,
        postUiCallback: ((T) -> Unit)? = null
    ) {
        binding.overlayView.addJob {
            val result = suspendCallback()
            runOnUiThread {
                postUiCallback?.invoke(result)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun showTotalCacheSizeOfCheckedPackages() {
        addOverlayJob(
            suspendCallback = {
                val totalCacheBytes = PlaceholderContent.Current.getCheckedTotalCacheSize()
                if (totalCacheBytes > 0L)
                    DataSize.ofBytes(totalCacheBytes).toFormattedString(this)
                else
                    null
            },
            postUiCallback = { sizeStr ->
                val title = getString(R.string.clear_cache_btn_text)
                sizeStr?.let {
                    supportActionBar?.title = String.format("%s (%s)", title, sizeStr)
                } ?: run {
                    supportActionBar?.title = title
                }
            }
        )
    }
}
