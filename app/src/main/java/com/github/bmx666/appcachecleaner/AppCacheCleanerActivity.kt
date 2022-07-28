package com.github.bmx666.appcachecleaner

import android.app.AlertDialog
import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ConditionVariable
import android.os.storage.StorageManager
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.databinding.ActivityMainBinding
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
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
            pkgInfoListFragment = getListInstalledUserApps()
            showPackageFragment()
        }

        binding.btnCleanSystemAppCache.setOnClickListener {
            pkgInfoListFragment = getListInstalledSystemApps()
            showPackageFragment()
        }

        binding.btnCleanAllAppCache.setOnClickListener {
            pkgInfoListFragment = getListInstalledAllApps()
            showPackageFragment()
        }

        binding.btnCloseApp.setOnClickListener {
            finish()
        }

        binding.fabCleanCache.setOnClickListener {
            addExtraSearchText(resources.configuration.locales.get(0))

            binding.fragmentContainerView.visibility = View.GONE
            binding.layoutFab.visibility = View.GONE
            binding.layoutButton.visibility = View.VISIBLE

            PlaceholderContent.ITEMS.filter { it.checked }.forEach { checkedPkgList.add(it.name) }
            PlaceholderContent.ITEMS.filter { !it.checked }.forEach { checkedPkgList.remove(it.name) }

            SharedPreferencesManager.PackageList.saveChecked(this, checkedPkgList)

            CoroutineScope(IO).launch {
                startCleanCache(PlaceholderContent.ITEMS.filter { it.checked }.map { it.name })
            }
        }

        binding.fabCheckAllApps.tag = "uncheck"
        binding.fabCheckAllApps.setOnClickListener {

            if (PlaceholderContent.ITEMS.all { it.checked })
                binding.fabCheckAllApps.tag = "uncheck"
            else if (PlaceholderContent.ITEMS.none { it.checked })
                binding.fabCheckAllApps.tag = "check"

            if (binding.fabCheckAllApps.tag.equals("uncheck")) {
                binding.fabCheckAllApps.tag = "check"
                binding.fabCheckAllApps.contentDescription = getString(R.string.description_apps_all_check)
                PlaceholderContent.ITEMS.forEach { it.checked = false }
            } else {
                binding.fabCheckAllApps.tag = "uncheck"
                binding.fabCheckAllApps.contentDescription = getString(R.string.description_apps_all_uncheck)
                PlaceholderContent.ITEMS.forEach { it.checked = true }
            }

            PlaceholderContent.ITEMS.forEach {
                if (it.checked)
                    checkedPkgList.add(it.name)
                else
                    checkedPkgList.remove(it.name)
            }

            PlaceholderContent.sort()

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view,
                    PackageListFragment.newInstance(),
                    FRAGMENT_PACKAGE_LIST_TAG)
                .commitNow()
        }

        binding.btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(intent)
        }

        binding.btnOpenUsageStats.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(intent)
        }

        binding.btnAddExtraSearchText.setOnClickListener {
            val locale = resources.configuration.locales.get(0)

            val inputEditTextClearCache = EditText(this)
            val clearCacheText = SharedPreferencesManager
                .ExtraSearchText.getClearCache(this, locale)
            if (clearCacheText?.isNotEmpty() == true)
                inputEditTextClearCache.setText(clearCacheText)
            else
                inputEditTextClearCache.hint = getText(R.string.clear_cache_btn_text)

            val dialogForClearCacheText = AlertDialog.Builder(this)
                .setTitle(getText(R.string.dialog_extra_search_text_title))
                .setMessage(getString(R.string.dialog_extra_search_text_message,
                            locale.displayLanguage, locale.displayCountry,
                            getText(R.string.clear_cache_btn_text)))
                .setView(inputEditTextClearCache)
                .setPositiveButton("OK") { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.saveClearCache(
                        this, locale, inputEditTextClearCache.text)
                }
                .setNegativeButton(getText(R.string.dialog_extra_search_text_btn_remove)) { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.removeClearCache(
                        this, locale)
                }
                .create()
            dialogForClearCacheText.show()

            val inputEditTextStorage = EditText(this)
            val storageText = SharedPreferencesManager
                .ExtraSearchText.getStorage(this, locale)
            if (storageText?.isNotEmpty() == true)
                inputEditTextStorage.setText(storageText)
            else
                inputEditTextStorage.hint =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        getText(R.string.storage_settings_for_app)
                    else
                        getText(R.string.storage_label)
            val dialogForStorageText = AlertDialog.Builder(this)
                .setTitle(getText(R.string.dialog_extra_search_text_title))
                .setMessage(getString(R.string.dialog_extra_search_text_message,
                    locale.displayLanguage, locale.displayCountry,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        getText(R.string.storage_settings_for_app)
                    else
                        getText(R.string.storage_label)))
                .setView(inputEditTextStorage)
                .setPositiveButton("OK") { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.saveStorage(
                        this, locale, inputEditTextStorage.text)
                }
                .setNegativeButton(getText(R.string.dialog_extra_search_text_btn_remove)) { _, _ ->
                    SharedPreferencesManager.ExtraSearchText.removeStorage(
                        this, locale)
                }
                .create()
            dialogForStorageText.show()
        }

        checkedPkgList.addAll(SharedPreferencesManager.PackageList.getChecked(this))

        if (checkAllRequiredPermissions(this))
            binding.textView.text = intent.getCharSequenceExtra(ARG_DISPLAY_TEXT)
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(Constant.Intent.DisableSelf.ACTION))
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (!cleanCacheFinished.get()) return

        val hasAccessibilityPermission = checkAccessibilityPermission(this)
        val hasUsageStatsPermission = checkUsageStatsPermission(this)
        // Usage stats permission is allow get cache size of apps only for Android 8 and later
        val ignoreUsageStatsPermission = (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)

        val hasAllPermissions = hasAccessibilityPermission and
                (hasUsageStatsPermission or ignoreUsageStatsPermission)

        if (!hasAccessibilityPermission) {
            Toast.makeText(this, getText(R.string.text_enable_accessibility_permission), Toast.LENGTH_SHORT).show()
            binding.textView.text = getText(R.string.text_enable_accessibility)
        } else if (!hasUsageStatsPermission and !ignoreUsageStatsPermission) {
            Toast.makeText(this, getText(R.string.text_enable_usage_stats_permission), Toast.LENGTH_SHORT).show()
            binding.textView.text = getText(R.string.text_enable_usage_stats)
        } else {
            binding.textView.text = intent.getCharSequenceExtra(ARG_DISPLAY_TEXT)
        }

        binding.btnOpenAccessibility.isEnabled = !hasAccessibilityPermission
        binding.btnOpenUsageStats.isEnabled = !hasUsageStatsPermission
        binding.btnCleanUserAppCache.isEnabled = hasAllPermissions
        binding.btnCleanSystemAppCache.isEnabled = hasAllPermissions
        binding.btnCleanAllAppCache.isEnabled = hasAllPermissions

        if (ignoreUsageStatsPermission)
            binding.btnOpenUsageStats.visibility = View.GONE
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

    private fun getListInstalledUserApps(): ArrayList<PackageInfo> {
        return getListInstalledApps(systemOnly = false, userOnly = true)
    }

    private fun getListInstalledSystemApps(): ArrayList<PackageInfo> {
        return getListInstalledApps(systemOnly = true, userOnly = false)
    }

    private fun getListInstalledAllApps(): ArrayList<PackageInfo> {
        return getListInstalledApps(systemOnly = true, userOnly = true)
    }

    private fun getListInstalledApps(systemOnly: Boolean, userOnly: Boolean): ArrayList<PackageInfo> {
        val list = packageManager.getInstalledPackages(0)
        val pkgInfoList = ArrayList<PackageInfo>()
        for (i in list.indices) {
            val packageInfo = list[i]
            val flags = packageInfo!!.applicationInfo.flags
            val isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val addPkg = (systemOnly && (isSystemApp and !isUpdatedSystemApp)) or
                            (userOnly && (!isSystemApp or isUpdatedSystemApp))
            if (addPkg)
                pkgInfoList.add(packageInfo)
        }
        return pkgInfoList
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
        PlaceholderContent.ITEMS.clear()
        pkgInfoListFragment.forEach { pkgInfo ->

            if (!loadingPkgList.get()) return

            var localizedLabel: String? = null
            var icon: Drawable? = null
            packageManager?.let { pm ->
                try {
                    icon = pm.getApplicationIcon(pkgInfo.packageName)
                    val res = pm.getResourcesForApplication(pkgInfo.applicationInfo)
                    val resId = pkgInfo.applicationInfo.labelRes
                    if (resId != 0)
                        localizedLabel = res.getString(resId)
                } catch (e: PackageManager.NameNotFoundException) {}
            }
            val label = localizedLabel
                ?: pkgInfo.applicationInfo.nonLocalizedLabel?.toString()
                ?: pkgInfo.packageName

            val stats = getStorageStats(pkgInfo.packageName)

            PlaceholderContent.addItem(pkgInfo, label, icon,
                checkedPkgList.contains(pkgInfo.packageName), stats)

            runOnUiThread {
                binding.progressBarPackageList.incrementProgressBy(1)
                binding.textProgressPackageList.text = String.format(Locale.getDefault(),
                    "%d / %d", PlaceholderContent.ITEMS.size,
                    pkgInfoListFragment.size)
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

    private fun getStorageStats(packageName: String): StorageStats? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        try {
            val storageStatsManager =
                getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            return storageStatsManager.queryStatsForPackage(
                StorageManager.UUID_DEFAULT, packageName,
                android.os.Process.myUserHandle()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
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

    private fun addExtraSearchText(locale: Locale) {
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