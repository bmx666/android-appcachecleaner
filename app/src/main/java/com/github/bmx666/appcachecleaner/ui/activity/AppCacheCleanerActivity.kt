package com.github.bmx666.appcachecleaner.ui.activity

import android.content.*
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileUtils
import android.provider.Settings
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.databinding.ActivityMainBinding
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.ui.dialog.ExtraSearchTextDialogBuilder
import com.github.bmx666.appcachecleaner.ui.dialog.PermissionDialogBuilder
import com.github.bmx666.appcachecleaner.ui.fragment.HelpFragment
import com.github.bmx666.appcachecleaner.ui.fragment.PackageListFragment
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AppCacheCleanerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val checkedPkgList: HashSet<String> = HashSet()
    private var pkgInfoListFragment: ArrayList<PackageInfo> = ArrayList()

    private val mLocalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.Intent.CleanCacheAppInfo.ACTION -> {
                    startApplicationDetailsActivity(
                        intent.getStringExtra(Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME)
                    )
                }
                Constant.Intent.CleanCacheFinish.ACTION -> {
                    cleanCacheFinish(
                        intent.getBooleanExtra(
                            Constant.Intent.CleanCacheFinish.NAME_INTERRUPTED,
                            false
                        )
                    )
                    if (BuildConfig.DEBUG)
                        saveLogFile()
                }
                Constant.Intent.StopAccessibilityServiceFeedback.ACTION -> {
                    runOnUiThread {
                        updateStartStopServiceButton()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constant.Intent.CleanCacheFinish.ACTION)
        intentFilter.addAction(Constant.Intent.CleanCacheAppInfo.ACTION)
        intentFilter.addAction(Constant.Intent.StopAccessibilityServiceFeedback.ACTION)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mLocalReceiver, intentFilter)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        onBackPressedDispatcher.addCallback(
            this, // lifecycle owner
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (loadingPkgList.get()) {
                        loadingPkgList.set(false)
                        hideFragmentViews()
                        showButtons()
                        return
                    }

                    supportFragmentManager.findFragmentByTag(FRAGMENT_CONTAINER_VIEW_TAG)
                        ?.let { fragment ->
                            hideFragmentViews()
                            supportFragmentManager.beginTransaction().remove(fragment).commitNow()
                            showButtons()
                            return
                        }
                }
            })

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
                disableAccessibilityService()
            else
                PermissionDialogBuilder.buildAccessibilityPermissionDialog(this)
        }

        binding.fabCleanCache.setOnClickListener {
            startCleanCache()
        }

        binding.fabCheckAllApps.tag = "uncheck"
        binding.fabCheckAllApps.setOnClickListener {

            if (PlaceholderContent.isAllCheckedVisible())
                binding.fabCheckAllApps.tag = "uncheck"
            else if (PlaceholderContent.isAllUncheckedVisible())
                binding.fabCheckAllApps.tag = "check"

            if (binding.fabCheckAllApps.tag.equals("uncheck")) {
                binding.fabCheckAllApps.tag = "check"
                binding.fabCheckAllApps.contentDescription =
                    getString(R.string.description_apps_all_check)
                PlaceholderContent.uncheckAllVisible()
            } else {
                binding.fabCheckAllApps.tag = "uncheck"
                binding.fabCheckAllApps.contentDescription =
                    getString(R.string.description_apps_all_uncheck)
                PlaceholderContent.checkAllVisible()
            }

            PlaceholderContent.getVisibleCheckedPackageList().forEach { checkedPkgList.add(it) }
            PlaceholderContent.getVisibleUncheckedPackageList()
                .forEach { checkedPkgList.remove(it) }

            PlaceholderContent.sort()

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container_view,
                    PackageListFragment.newInstance(),
                    FRAGMENT_CONTAINER_VIEW_TAG
                )
                .commitNow()
        }

        checkedPkgList.addAll(SharedPreferencesManager.PackageList.getChecked(this))

        if (PermissionChecker.checkAllRequiredPermissions(this))
            binding.textView.text = intent.getCharSequenceExtra(ARG_DISPLAY_TEXT)
        else
            checkAndShowPermissionDialogs()

        updateStartStopServiceButton()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_extra_search_text_storage -> {
                showExtraSearchTextDialogForStorage()
                true
            }
            R.id.add_extra_search_text_clear_cache -> {
                showExtraSearchTextDialogForClearCache()
                true
            }
            R.id.help -> {
                showHelp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStartStopServiceButton()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver)
        super.onDestroy()
    }

    private fun startCleanCache() {
        val pkgList = PlaceholderContent.getVisibleCheckedPackageList().toMutableList()

        addExtraSearchText()

        hideFragmentViews()
        showButtons()

        // ignore empty list and show main screen
        if (pkgList.isEmpty()) {
            binding.textView.text = ""
            return
        }

        // clear cache of app in the end to avoid issues
        if (pkgList.contains(packageName)) {
            pkgList.remove(packageName)
            // cache dir is using for log file in debug version
            // clean cache dir in release only
            if (!BuildConfig.DEBUG)
                pkgList.add(packageName)
        }

        val intent = Intent(Constant.Intent.ClearCache.ACTION)
        intent.putStringArrayListExtra(
            Constant.Intent.ClearCache.NAME_PACKAGE_LIST,
            pkgList as ArrayList<String>
        )
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

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

    private fun cleanCacheFinish(cleanCacheInterrupted: Boolean) {

        val cleanCacheBytes =
            PlaceholderContent.getItems().filter { it.checked }.sumOf {
                PackageManagerHelper.getCacheSizeDiff(
                    it.stats,
                    PackageManagerHelper.getStorageStats(this, it.pkgInfo)
                )
            }

        val displayText =
            if (cleanCacheInterrupted)
                getString(
                    R.string.text_clean_cache_interrupt,
                    Formatter.formatFileSize(this, cleanCacheBytes)
                )
            else
                getString(
                    R.string.text_clean_cache_finish,
                    Formatter.formatFileSize(this, cleanCacheBytes)
                )

        runOnUiThread {
            binding.textView.text = displayText
            updateStartStopServiceButton()
        }

        // return back to Main Activity, sometimes not possible press Back from Settings
        val intent = this.intent
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        intent.putExtra(ARG_DISPLAY_TEXT, displayText)
        startActivity(intent)
    }

    private fun addPackageToPlaceholderContent() {
        val locale = getCurrentLocale()

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

        PlaceholderContent.sort()

        runOnUiThread {
            binding.layoutProgress.visibility = View.GONE
            binding.fragmentContainerView.visibility = View.VISIBLE
            binding.layoutFab.visibility = View.VISIBLE

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container_view,
                    PackageListFragment.newInstance(),
                    FRAGMENT_CONTAINER_VIEW_TAG
                )
                .commitNow()

            loadingPkgList.set(false)
        }
    }

    private fun showPackageFragment() {
        hideCleanButtons()

        binding.textProgressPackageList.text = String.format(
            Locale.getDefault(),
            "%d / %d", 0, pkgInfoListFragment.size
        )
        binding.progressBarPackageList.progress = 0
        binding.progressBarPackageList.max = pkgInfoListFragment.size
        binding.layoutProgress.visibility = View.VISIBLE

        loadingPkgList.set(true)

        CoroutineScope(Dispatchers.IO).launch {
            addPackageToPlaceholderContent()
        }
    }

    private fun addExtraSearchText() {
        val locale = getCurrentLocale()

        val intent = Intent(Constant.Intent.ExtraSearchText.ACTION)

        SharedPreferencesManager.ExtraSearchText.getClearCache(this, locale)?.let { value ->
            if (value.isNotEmpty())
                intent.putExtra(Constant.Intent.ExtraSearchText.NAME_CLEAR_CACHE, value)
        }

        SharedPreferencesManager.ExtraSearchText.getStorage(this, locale)?.let { value ->
            if (value.isNotEmpty())
                intent.putExtra(Constant.Intent.ExtraSearchText.NAME_STORAGE, value)
        }

        intent.extras?.let {
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(intent)
        }
    }

    private fun showExtraSearchTextDialogForStorage() {
        val locale = getCurrentLocale()
        ExtraSearchTextDialogBuilder.buildStorageDialog(this, locale)
    }

    private fun showExtraSearchTextDialogForClearCache() {
        val locale = getCurrentLocale()
        ExtraSearchTextDialogBuilder.buildClearCacheDialog(this, locale)
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

    fun startApplicationDetailsActivity(packageName: String?) {
        // everything is possible...
        if (packageName == null || packageName.trim().isEmpty()) return
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) return@registerForActivityResult
        startApplicationDetailsActivity(this.packageName)
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

    private fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            resources.configuration.locales.get(0)
        else
            resources.configuration.locale
    }

    private fun hideFragmentViews() {
        binding.fragmentContainerView.visibility = View.GONE
        binding.layoutFab.visibility = View.GONE
        binding.layoutProgress.visibility = View.GONE
    }

    private fun showButtons() {
        binding.layoutButton.visibility = View.VISIBLE
    }

    private fun hideCleanButtons() {
        binding.layoutButton.visibility = View.GONE
    }

    private fun showHelp() {
        hideFragmentViews()
        hideCleanButtons()
        binding.fragmentContainerView.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container_view,
                HelpFragment.newInstance(),
                FRAGMENT_CONTAINER_VIEW_TAG
            )
            .commitNow()
    }

    private fun updateStartStopServiceButton() {
        if (PermissionChecker.checkAccessibilityPermission(this))
            binding.btnStartStopService.setText(R.string.btn_stop_accessibility_service)
        else
            binding.btnStartStopService.setText(R.string.btn_start_accessibility_service)
    }

    private fun disableAccessibilityService() {
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(Constant.Intent.StopAccessibilityService.ACTION))

        // Android 6 doesn't have methods to disable Accessibility service
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(intent)
        }
    }

    companion object {
        const val ARG_DISPLAY_TEXT = "display-text"
        const val FRAGMENT_CONTAINER_VIEW_TAG = "fragment-container-view-tag"

        val loadingPkgList = AtomicBoolean(false)
    }
}