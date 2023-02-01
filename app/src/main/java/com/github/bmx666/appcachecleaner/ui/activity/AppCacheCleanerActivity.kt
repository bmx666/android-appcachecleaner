package com.github.bmx666.appcachecleaner.ui.activity

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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.databinding.ActivityMainBinding
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
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
    private val checkedPkgList: HashSet<String> = HashSet()
    private var pkgInfoListFragment: ArrayList<PackageInfo> = ArrayList()

    private lateinit var localBroadcastManager: LocalBroadcastManagerActivityHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            pkgInfoListFragment = PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = false,
                systemUpdated = true,
                userOnly = true,
            )
            showPackageFragment()
        }

        binding.btnCleanSystemAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            pkgInfoListFragment = PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = true,
                systemUpdated = false,
                userOnly = false,
            )
            showPackageFragment()
        }

        binding.btnCleanAllAppCache.setOnClickListener {
            if (!checkAndShowPermissionDialogs()) return@setOnClickListener

            pkgInfoListFragment = PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = true,
                systemUpdated = true,
                userOnly = true,
            )
            showPackageFragment()
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
            startCleanCache()
        }

        binding.fabCheckAllApps.setOnClickListener {
            when (
                if (PlaceholderContent.isAllCheckedVisible())
                    "uncheck"
                else if (PlaceholderContent.isAllUncheckedVisible())
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

            PlaceholderContent.getVisibleCheckedPackageList()
                .forEach { checkedPkgList.add(it) }
            PlaceholderContent.getVisibleUncheckedPackageList()
                .forEach { checkedPkgList.remove(it) }

            PlaceholderContent.sort()

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container_view,
                    PackageListFragment.newInstance(),
                    FRAGMENT_CONTAINER_VIEW_TAG
                )
                .commitNowAllowingStateLoss()
        }

        updateMainText(intent.getCharSequenceExtra(ARG_DISPLAY_TEXT))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.help -> {
                showMenuFragment(HelpFragment.newInstance(), R.string.menu_item_help)
                true
            }
            R.id.settings -> {
                showMenuFragment(SettingsFragment.newInstance(), R.string.menu_item_settings)
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

    private fun startCleanCache() {
        val pkgList = PlaceholderContent.getVisibleCheckedPackageList().toMutableList()

        addExtraSearchText()

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

        CoroutineScope(Dispatchers.IO).launch {
            PlaceholderContent.getVisibleCheckedPackageList().forEach { checkedPkgList.add(it) }
            PlaceholderContent.getVisibleUncheckedPackageList()
                .forEach { checkedPkgList.remove(it) }

            SharedPreferencesManager.PackageList.saveChecked(
                this@AppCacheCleanerActivity,
                checkedPkgList
            )
        }
    }

    private fun addPackageToPlaceholderContent() {
        val locale = LocaleHelper.getCurrentLocale(this)

        PlaceholderContent.reset()

        var progressApps = 0
        val totalApps = pkgInfoListFragment.size

        pkgInfoListFragment.forEach { pkgInfo ->

            if (!loadingPkgList.get()) return

            val stats = PackageManagerHelper.getStorageStats(this, pkgInfo)

            if (PlaceholderContent.contains(pkgInfo)) {
                PlaceholderContent.updateStats(pkgInfo, stats)
                if (!PlaceholderContent.isSameLabelLocale(pkgInfo, locale)) {
                    val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                    PlaceholderContent.updateLabel(pkgInfo, label, locale)
                }
            } else {
                val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                val checked = checkedPkgList.contains(pkgInfo.packageName)
                PlaceholderContent.addItem(pkgInfo, label, locale, checked, stats)
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
        PlaceholderContent.sort()

        if (!loadingPkgList.get()) return
        runOnUiThread {
            binding.layoutProgress.visibility = View.GONE
            binding.fragmentContainerView.visibility = View.VISIBLE
            binding.layoutFab.visibility = View.VISIBLE
            binding.fabCheckAllApps.tag = "uncheck"

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container_view,
                    PackageListFragment.newInstance(),
                    FRAGMENT_CONTAINER_VIEW_TAG
                )
                .commitNowAllowingStateLoss()
        }
        loadingPkgList.set(false)
    }

    private fun showPackageFragment() {
        hideFragmentViews()
        hideMainViews()
        updateActionBar(R.string.clear_cache_btn_text)

        binding.textProgressPackageList.text = String.format(
            Locale.getDefault(),
            "%d / %d", 0, pkgInfoListFragment.size
        )
        binding.progressBarPackageList.progress = 0
        binding.progressBarPackageList.max = pkgInfoListFragment.size
        binding.layoutProgress.visibility = View.VISIBLE

        loadingPkgList.set(true)

        CoroutineScope(Dispatchers.IO).launch {
            val pkgList = SharedPreferencesManager.PackageList.getChecked(this@AppCacheCleanerActivity)
            checkedPkgList.addAll(pkgList)
            addPackageToPlaceholderContent()
        }
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
        // interrupt to load package list
        loadingPkgList.set(false)

        restoreActionBar()
        binding.fragmentContainerView.visibility = View.GONE
        binding.layoutFab.visibility = View.GONE
        binding.layoutProgress.visibility = View.GONE
    }

    private fun showMainViews() {
        binding.layoutButton.visibility = View.VISIBLE
        updateExtraButtonsVisibility()
        updateStartStopServiceButton()
    }

    private fun hideMainViews() {
        binding.layoutButton.visibility = View.GONE
    }

    private fun updateActionBar(@StringRes title: Int) {
        supportActionBar?.setTitle(title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun restoreActionBar() {
        supportActionBar?.setTitle(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
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
        updateActionBar(title)
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

    private fun handleOnBackPressed() {
        if (loadingPkgList.get()) {
            hideFragmentViews()
            showMainViews()
            return
        }

        supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
            ?.let { fragment ->
                restoreActionBar()
                hideFragmentViews()
                supportFragmentManager.beginTransaction().remove(fragment).commitNow()
                showMainViews()
                return
            }
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