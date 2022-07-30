package com.github.bmx666.appcachecleaner

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ConditionVariable
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.databinding.ActivityMainBinding
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.PermissionChecker.Companion.checkAccessibilityPermission
import com.github.bmx666.appcachecleaner.util.PermissionChecker.Companion.checkAllRequiredPermissions
import com.github.bmx666.appcachecleaner.util.PermissionChecker.Companion.checkUsageStatsPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


class AppCacheCleanerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val checkedPkgList: HashSet<String> = HashSet()
    private var pkgInfoListFragment: ArrayList<PackageInfo> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.btnCleanUserAppCache.setOnClickListener {
            if (!checkAndDisplayPermissionDialogs()) return@setOnClickListener

            pkgInfoListFragment = PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = false,
                systemUpdated = true,
                userOnly = true,
            )
            showPackageFragment()
        }

        binding.btnCleanSystemAppCache.setOnClickListener {
            if (!checkAndDisplayPermissionDialogs()) return@setOnClickListener

            pkgInfoListFragment = PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = true,
                systemUpdated = false,
                userOnly = false,
            )
            showPackageFragment()
        }

        binding.btnCleanAllAppCache.setOnClickListener {
            if (!checkAndDisplayPermissionDialogs()) return@setOnClickListener

            pkgInfoListFragment = PackageManagerHelper.getInstalledApps(
                context = this,
                systemNotUpdated = true,
                systemUpdated = true,
                userOnly = true,
            )
            showPackageFragment()
        }

        binding.btnCloseApp.setOnClickListener {
            finish()
        }

        binding.fabCleanCache.setOnClickListener {
            addExtraSearchText()

            binding.fragmentContainerView.visibility = View.GONE
            binding.layoutFab.visibility = View.GONE
            binding.layoutButton.visibility = View.VISIBLE

            PlaceholderContent.getVisibleCheckedPackageList().forEach { checkedPkgList.add(it) }
            PlaceholderContent.getVisibleUncheckedPackageList().forEach { checkedPkgList.remove(it) }

            SharedPreferencesManager.PackageList.saveChecked(this, checkedPkgList)

            CoroutineScope(IO).launch {
                startCleanCache(PlaceholderContent.getVisibleCheckedPackageList())
            }
        }

        binding.fabCheckAllApps.tag = "uncheck"
        binding.fabCheckAllApps.setOnClickListener {

            if (PlaceholderContent.isAllCheckedVisible())
                binding.fabCheckAllApps.tag = "uncheck"
            else if (PlaceholderContent.isAllUncheckedVisible())
                binding.fabCheckAllApps.tag = "check"

            if (binding.fabCheckAllApps.tag.equals("uncheck")) {
                binding.fabCheckAllApps.tag = "check"
                binding.fabCheckAllApps.contentDescription = getString(R.string.description_apps_all_check)
                PlaceholderContent.uncheckAllVisible()
            } else {
                binding.fabCheckAllApps.tag = "uncheck"
                binding.fabCheckAllApps.contentDescription = getString(R.string.description_apps_all_uncheck)
                PlaceholderContent.checkAllVisible()
            }

            PlaceholderContent.getVisibleCheckedPackageList().forEach { checkedPkgList.add(it) }
            PlaceholderContent.getVisibleUncheckedPackageList().forEach { checkedPkgList.remove(it) }

            PlaceholderContent.sort()

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view,
                    PackageListFragment.newInstance(),
                    FRAGMENT_PACKAGE_LIST_TAG)
                .commitNow()
        }

        checkedPkgList.addAll(SharedPreferencesManager.PackageList.getChecked(this))

        if (checkAllRequiredPermissions(this))
            binding.textView.text = intent.getCharSequenceExtra(ARG_DISPLAY_TEXT)
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
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(Constant.Intent.DisableSelf.ACTION))
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (loadingPkgList.get()) {
            loadingPkgList.set(false)

            binding.fragmentContainerView.visibility = View.GONE
            binding.layoutFab.visibility = View.GONE
            binding.layoutProgress.visibility = View.GONE

            binding.btnCleanUserAppCache.isEnabled = true
            binding.btnCleanSystemAppCache.isEnabled = true
            binding.btnCleanAllAppCache.isEnabled = true

            binding.layoutButton.visibility = View.VISIBLE

            return
        }

        supportFragmentManager.findFragmentByTag(FRAGMENT_PACKAGE_LIST_TAG)?.let { fragment ->

            binding.fragmentContainerView.visibility = View.GONE
            binding.layoutFab.visibility = View.GONE

            supportFragmentManager.beginTransaction().remove(fragment).commitNow()

            binding.btnCleanUserAppCache.isEnabled = true
            binding.btnCleanSystemAppCache.isEnabled = true
            binding.btnCleanAllAppCache.isEnabled = true

            binding.layoutButton.visibility = View.VISIBLE

            return
        }
        super.onBackPressed()
    }

    private suspend fun startCleanCache(pkgList: List<String>) {
        cleanCacheInterrupt.set(false)
        cleanCacheFinished.set(false)

        for (i in pkgList.indices) {
            startApplicationDetailsActivity(pkgList[i])
            cleanAppCacheFinished.set(false)
            runOnUiThread {
                binding.textView.text = String.format(Locale.getDefault(),
                    "%d / %d %s", i, pkgList.size,
                    getText(R.string.text_clean_cache_left))
            }
            delay(500L)
            waitAccessibility.block(5000L)
            delay(500L)

            // user interrupt process
            if (cleanCacheInterrupt.get()) break
        }
        cleanCacheFinished.set(true)

        runOnUiThread {
            val displayText = if (cleanCacheInterrupt.get())
                getText(R.string.text_clean_cache_interrupt)
                else getText(R.string.text_clean_cache_finish)
            binding.textView.text = displayText

            binding.btnCleanUserAppCache.isEnabled = true
            binding.btnCleanSystemAppCache.isEnabled = true
            binding.btnCleanAllAppCache.isEnabled = true

            // return back to Main Activity, sometimes not possible press Back from Settings
            if (pkgList.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val intent = this.intent
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra(ARG_DISPLAY_TEXT, displayText)
                startActivity(intent)
            }
        }
    }

    private fun startApplicationDetailsActivity(packageName: String) {
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
                val icon = PackageManagerHelper.getApplicationIcon(this, pkgInfo)
                val label = PackageManagerHelper.getApplicationLabel(this, pkgInfo)
                val checked = checkedPkgList.contains(pkgInfo.packageName)
                PlaceholderContent.addItem(pkgInfo, label, locale, icon, checked, stats)
            }

            progressApps += 1

            runOnUiThread {
                binding.progressBarPackageList.incrementProgressBy(1)
                binding.textProgressPackageList.text = String.format(Locale.getDefault(),
                    "%d / %d", progressApps, totalApps)
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
                    FRAGMENT_PACKAGE_LIST_TAG
                )
                .commitNow()

            loadingPkgList.set(false)
        }
    }

    private fun showPackageFragment() {
        binding.btnCleanUserAppCache.isEnabled = false
        binding.btnCleanSystemAppCache.isEnabled = false
        binding.btnCleanAllAppCache.isEnabled = false

        binding.layoutButton.visibility = View.GONE

        binding.textProgressPackageList.text = String.format(Locale.getDefault(),
            "%d / %d", 0, pkgInfoListFragment.size)
        binding.progressBarPackageList.progress = 0
        binding.progressBarPackageList.max = pkgInfoListFragment.size
        binding.layoutProgress.visibility = View.VISIBLE

        loadingPkgList.set(true)

        CoroutineScope(IO).launch {
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

        val inputEditText = EditText(this)

        val text = SharedPreferencesManager.ExtraSearchText.getStorage(this, locale)
        if (text?.isNotEmpty() == true)
            inputEditText.setText(text)
        else
            inputEditText.hint =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    getText(R.string.storage_settings_for_app)
                else
                    getText(R.string.storage_label)

        AlertDialog.Builder(this)
            .setTitle(getText(R.string.dialog_extra_search_text_title))
            .setMessage(getString(R.string.dialog_extra_search_text_message,
                locale.displayLanguage, locale.displayCountry,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    getText(R.string.storage_settings_for_app)
                else
                    getText(R.string.storage_label)))
            .setView(inputEditText)
            .setPositiveButton("OK") { _, _ ->
                SharedPreferencesManager.ExtraSearchText.saveStorage(
                    this, locale, inputEditText.text)
            }
            .setNegativeButton(getText(R.string.dialog_extra_search_text_btn_remove)) { _, _ ->
                SharedPreferencesManager.ExtraSearchText.removeStorage(
                    this, locale)
            }
            .create()
            .show()
    }

    private fun showExtraSearchTextDialogForClearCache() {
        val locale = getCurrentLocale()

        val inputEditText = EditText(this)

        val text = SharedPreferencesManager.ExtraSearchText.getClearCache(this, locale)
        if (text?.isNotEmpty() == true)
            inputEditText.setText(text)
        else
            inputEditText.hint = getText(R.string.clear_cache_btn_text)

        AlertDialog.Builder(this)
            .setTitle(getText(R.string.dialog_extra_search_text_title))
            .setMessage(getString(R.string.dialog_extra_search_text_message,
                locale.displayLanguage, locale.displayCountry,
                getText(R.string.clear_cache_btn_text)))
            .setView(inputEditText)
            .setPositiveButton("OK") { _, _ ->
                SharedPreferencesManager.ExtraSearchText.saveClearCache(
                    this, locale, inputEditText.text)
            }
            .setNegativeButton(getText(R.string.dialog_extra_search_text_btn_remove)) { _, _ ->
                SharedPreferencesManager.ExtraSearchText.removeClearCache(
                    this, locale)
            }
            .create()
            .show()
    }

    private fun checkAndDisplayPermissionDialogs(): Boolean {
        val hasAccessibilityPermission = checkAccessibilityPermission(this)

        if (!hasAccessibilityPermission) {
            AlertDialog.Builder(this)
                .setTitle(getText(R.string.text_enable_accessibility_permission))
                .setMessage(getString(R.string.text_enable_accessibility))
                .setPositiveButton("OK") { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(intent)
                }
                .create()
                .show()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return checkAccessibilityPermission(this)

        val hasUsageStatsPermission = checkUsageStatsPermission(this)

        // Usage stats permission is allow get cache size of apps only for Android 8 and later
        if (!hasUsageStatsPermission) {
            AlertDialog.Builder(this)
                .setTitle(getText(R.string.text_enable_usage_stats_permission))
                .setMessage(getString(R.string.text_enable_usage_stats))
                .setPositiveButton("OK") { _, _ ->
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(intent)
                }
                .create()
                .show()
        }

        return checkAccessibilityPermission(this) && checkUsageStatsPermission(this)
    }

    private fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                resources.configuration.locales.get(0)
            else
                resources.configuration.locale
    }

    companion object {

        const val ARG_DISPLAY_TEXT = "display-text"

        const val FRAGMENT_PACKAGE_LIST_TAG = "package-list"

        val loadingPkgList = AtomicBoolean(false)
        val cleanAppCacheFinished = AtomicBoolean(false)
        val cleanCacheFinished = AtomicBoolean(true)
        val cleanCacheInterrupt = AtomicBoolean(false)
        val waitAccessibility = ConditionVariable()
    }
}