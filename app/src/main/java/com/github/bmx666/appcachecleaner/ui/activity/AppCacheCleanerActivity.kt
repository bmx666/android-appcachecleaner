package com.github.bmx666.appcachecleaner.ui.activity

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import android.os.FileUtils
import android.text.format.Formatter
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
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.ui.dialog.CustomListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.FilterListDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.FirstBootDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.PermissionDialogBuilder
import com.github.bmx666.appcachecleaner.ui.fragment.HelpFragment
import com.github.bmx666.appcachecleaner.ui.fragment.PackageListFragment
import com.github.bmx666.appcachecleaner.ui.fragment.SettingsFragment
import com.github.bmx666.appcachecleaner.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AppCacheCleanerActivity : AppCompatActivity(), IIntentActivityCallback {

    companion object {
        const val ARG_DISPLAY_TEXT = "display-text"
        const val FRAGMENT_CONTAINER_VIEW_TAG = "fragment-container-view-tag"

        val loadingPkgList = AtomicBoolean(false)
    }

    private lateinit var binding: ActivityMainBinding
    private var customListName: String? = null

    private lateinit var onMenuShowMain: () -> Unit
    private lateinit var onMenuShowFilter: () -> Unit
    private lateinit var onMenuShowSearch: () -> Unit

    private lateinit var localBroadcastManager: LocalBroadcastManagerActivityHelper

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
                PermissionDialogBuilder.buildAccessibilityPermissionDialog(this)
        }

        binding.btnCloseApp.setOnClickListener {
            if (PermissionChecker.checkAccessibilityPermission(this))
                localBroadcastManager.disableAccessibilityService()
            finish()
        }

        binding.fabCleanCache.setOnClickListener {
            val pkgList = PlaceholderContent.getAllChecked().toMutableList()
            startCleanCache(pkgList)
        }

        binding.fabCheckAllApps.setOnClickListener {
            when (
                if (PlaceholderContent.isAllVisibleChecked())
                    "uncheck"
                else if (PlaceholderContent.isAllVisibleUnchecked())
                    "check"
                else
                    binding.fabCheckAllApps.tag
            ) {
                "uncheck" -> {
                    binding.fabCheckAllApps.tag = "check"
                    binding.fabCheckAllApps.contentDescription =
                        getString(R.string.description_apps_all_check)
                    PlaceholderContent.uncheckAllVisible()
                }
                "check" -> {
                    binding.fabCheckAllApps.tag = "uncheck"
                    binding.fabCheckAllApps.contentDescription =
                        getString(R.string.description_apps_all_uncheck)
                    PlaceholderContent.checkAllVisible()
                }
            }

            supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                ?.let { fragment ->
                    if (fragment is PackageListFragment)
                        fragment.refreshAdapter()
                }
        }

        binding.fabCustomListOk.setOnClickListener {
            val checkedPkgList = PlaceholderContent.getAllChecked().toSet()
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
            }
        }

        updateMainText(intent.getCharSequenceExtra(ARG_DISPLAY_TEXT))

        if (SharedPreferencesManager.FirstBoot.showDialogHelpCustomizedSettingsUI(this))
            FirstBootDialogBuilder.buildHelpCustomizedSettingsUIDialog(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)

        onMenuShowMain = {
            menu.findItem(R.id.menu_help).isVisible = true
            menu.findItem(R.id.menu_settings).isVisible = true
            menu.findItem(R.id.menu_filter).isVisible = false
            menu.findItem(R.id.menu_search).isVisible = false
        }

        onMenuShowFilter = {
            menu.findItem(R.id.menu_help).isVisible = false
            menu.findItem(R.id.menu_settings).isVisible = false
            menu.findItem(R.id.menu_filter).isVisible =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            menu.findItem(R.id.menu_search).isVisible = false
        }

        onMenuShowSearch = {
            menu.findItem(R.id.menu_help).isVisible = false
            menu.findItem(R.id.menu_settings).isVisible = false
            menu.findItem(R.id.menu_filter).isVisible = false
            menu.findItem(R.id.menu_search).isVisible = true
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
                PlaceholderContent.getItems().forEach { it.ignore = false }
                PlaceholderContent.sortByLabel()
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
        localBroadcastManager.onDestroy()
        super.onDestroy()
    }

    private fun startCleanCache(pkgList: MutableList<String>) {
        addExtraSearchText()
        setScenario()

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

        val maxWaitAppTimeout = SharedPreferencesManager.Settings.getMaxWaitAppTimeout(this)
        localBroadcastManager.sendPackageList(pkgList as ArrayList<String>, maxWaitAppTimeout)
    }

    private fun addPackageToPlaceholderContent(pkgInfoList: ArrayList<PackageInfo>,
                                               isCustomListClearOnly: Boolean) {
        val locale = LocaleHelper.getCurrentLocale(this)

        var progressApps = 0
        val totalApps = pkgInfoList.size

        PlaceholderContent.resetAll()

        pkgInfoList.forEach { pkgInfo ->

            if (!loadingPkgList.get()) return

            // skip getting stats if custom list is loaded
            val stats = customListName?.let { null }
                ?: PackageManagerHelper.getStorageStats(this, pkgInfo)

            // update only stats if run cleaning process of custom list
            if (isCustomListClearOnly) {
                if (PlaceholderContent.contains(pkgInfo)) {
                    PlaceholderContent.updateStats(pkgInfo, stats)
                } else {
                    // skip getting the label of app it can take a lot of time on old phones
                    PlaceholderContent.addItem(pkgInfo, pkgInfo.packageName, locale, stats)
                }
            } else {
                if (PlaceholderContent.contains(pkgInfo)) {
                    PlaceholderContent.updateStats(pkgInfo, stats)
                    if (!PlaceholderContent.isSameLabelLocale(pkgInfo, locale)) {
                        val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                        PlaceholderContent.updateLabel(pkgInfo, label, locale)
                    }
                } else {
                    val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                    PlaceholderContent.addItem(pkgInfo, label, locale, stats)
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

        if (!loadingPkgList.get()) return

        when (customListName) {
            null -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    PlaceholderContent.filterByCacheSize(
                        SharedPreferencesManager.Filter.getMinCacheSize(this))
                else
                    PlaceholderContent.sort()
            }
            else -> {
                val checkedPkgList = SharedPreferencesManager.PackageList.get(this, customListName!!)
                PlaceholderContent.check(checkedPkgList)
                PlaceholderContent.sortByLabel()
            }
        }

        if (!loadingPkgList.get()) return

        runOnUiThread {
            if (isCustomListClearOnly)
                startCleanCache(PlaceholderContent.getAllChecked().toMutableList())
            else
                showPackageFragment()
        }

        loadingPkgList.set(false)
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

        loadingPkgList.set(true)

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

    private fun addExtraSearchText() {
        val intent = Intent(Constant.Intent.ExtraSearchText.ACTION)

        ExtraSearchTextHelper.getTextForClearCache(this).let { list ->
            if (list.isNotEmpty())
                intent.putExtra(Constant.Intent.ExtraSearchText.NAME_CLEAR_CACHE_TEXT_LIST, list)
        }

        ExtraSearchTextHelper.getTextForStorage(this).let { list ->
            if (list.isNotEmpty())
                intent.putExtra(Constant.Intent.ExtraSearchText.NAME_STORAGE_TEXT_LIST, list)
        }

        intent.extras?.let {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    private fun setScenario() {
        val intent = Intent(Constant.Intent.Scenario.ACTION)
        intent.putExtra(
            Constant.Intent.Scenario.NAME_TYPE,
            SharedPreferencesManager.Settings.getScenario(this))
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun checkAndShowPermissionDialogs(): Boolean {
        val hasAccessibilityPermission = PermissionChecker.checkAccessibilityPermission(this)
        if (!hasAccessibilityPermission) {
            PermissionDialogBuilder.buildAccessibilityPermissionDialog(this)
            return false
        }

        // Usage stats permission is allow get cache size of apps only for Android 8 and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasUsageStatsPermission = PermissionChecker.checkUsageStatsPermission(this)
            if (!hasUsageStatsPermission) {
                PermissionDialogBuilder.buildUsageStatsPermissionDialog(this)
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
                    )
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
        loadingPkgList.set(false)

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
            is HelpFragment -> updateActionBarMenu(R.string.menu_item_help)
            is SettingsFragment -> updateActionBarMenu(R.string.menu_item_settings)
            else -> restoreActionBar()
        }
    }

    private fun updateActionBarPackageList() {
        customListName?.let {
            updateActionBarSearch(customListName)
        } ?: updateActionBarFilter(R.string.clear_cache_btn_text)
    }

    private fun updateActionBarMenu(@StringRes resId: Int) {
        supportActionBar?.setTitle(resId)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        onMenuShowMain()
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
        updateActionBarMenu(title)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showFilterDialog() {
        FilterListDialogBuilder.buildMinCacheSizeDialog(this) { str ->
            val value =
                try { str?.toFloat() ?: 0.0f }
                catch (e: NumberFormatException) { 0.0f }
            if (!value.isFinite() or (value < 0.0f)) return@buildMinCacheSizeDialog
            val minCacheBytes = (value * 1024f * 1024f).toLong()
            supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                ?.let { fragment ->
                    if (fragment is PackageListFragment)
                        fragment.swapAdapterFilterByCacheBytes(minCacheBytes)
                }
        }
    }

    private fun updateExtraButtonsVisibility() {
        binding.btnStartStopService.visibility =
            when (SharedPreferencesManager.ExtraButtons.getShowStartStopService(this)) {
                true -> View.VISIBLE
                else -> View.GONE
            }

        binding.btnCloseApp.visibility =
            when (SharedPreferencesManager.ExtraButtons.getShowCloseApp(this)) {
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
        loadingPkgList.set(false)

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
        val cleanCacheBytes =
            PlaceholderContent.getItems().filter { it.checked }.sumOf {
                PackageManagerHelper.getCacheSizeDiff(
                    it.stats,
                    PackageManagerHelper.getStorageStats(this, it.pkgInfo)
                )
            }

        val resId = when (interrupted) {
            true -> R.string.text_clean_cache_interrupt
            else -> R.string.text_clean_cache_finish
        }

        val displayText = getString(resId,
            Formatter.formatFileSize(this, cleanCacheBytes))

        updateMainText(displayText)
        updateStartStopServiceButton()

        // return back to Main Activity, sometimes not possible press Back from Settings
        ActivityHelper.returnBackToMainActivity(this,
            this.intent.putExtra(ARG_DISPLAY_TEXT, displayText))

        if (BuildConfig.DEBUG)
            saveLogFile()
    }

    override fun onStopAccessibilityServiceFeedback() {
        updateStartStopServiceButton()
    }
}