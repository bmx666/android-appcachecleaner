package com.github.bmx666.appcachecleaner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.os.ConditionVariable
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.bmx666.appcachecleaner.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AppCacheCleanerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.btnCleanUserAppCache.setOnClickListener {
            binding.btnOpenAccessibility.isEnabled = false
            binding.btnCleanUserAppCache.isEnabled = false
            binding.btnCleanSystemAppCache.isEnabled = false
            binding.btnCleanAllAppCache.isEnabled = false

            CoroutineScope(IO).launch {
                startCleanCache(getListInstalledUserApps())
            }
        }

        binding.btnCleanSystemAppCache.setOnClickListener {
            binding.btnOpenAccessibility.isEnabled = false
            binding.btnCleanUserAppCache.isEnabled = false
            binding.btnCleanSystemAppCache.isEnabled = false
            binding.btnCleanAllAppCache.isEnabled = false
            CoroutineScope(IO).launch {
                startCleanCache(getListInstalledSystemApps())
            }
        }

        binding.btnCleanAllAppCache.setOnClickListener {
            binding.btnOpenAccessibility.isEnabled = false
            binding.btnCleanUserAppCache.isEnabled = false
            binding.btnCleanSystemAppCache.isEnabled = false
            binding.btnCleanAllAppCache.isEnabled = false
            CoroutineScope(IO).launch {
                startCleanCache(
                    getListInstalledUserApps() +
                    getListInstalledSystemApps())
            }
        }

        binding.btnOpenAccessibility.setOnClickListener {
            // if not construct intent to request permission
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // request permission via start activity for result
            startActivity(intent)
        }

        if (checkAccessibilityPermission())
            binding.textView.text = ""
    }

    override fun onResume() {
        super.onResume()

        if (!cleanCacheFinished.get()) return

        if (!checkAccessibilityPermission()) {
            Toast.makeText(this, getText(R.string.text_enable_accessibility_permission), Toast.LENGTH_SHORT).show()
            binding.textView.text = getText(R.string.text_enable_accessibility)
            binding.btnCleanUserAppCache.isEnabled = false
            binding.btnCleanSystemAppCache.isEnabled = false
            binding.btnCleanAllAppCache.isEnabled = false
        } else {
            binding.btnCleanUserAppCache.isEnabled = true
            binding.btnCleanSystemAppCache.isEnabled = true
            binding.btnCleanAllAppCache.isEnabled = true
        }
    }

    private suspend fun startCleanCache(pkgInfoList: List<PackageInfo>) {
        cleanCacheInterrupt.set(false)
        cleanCacheFinished.set(false)

        for (i in pkgInfoList.indices) {
            val pkgName = pkgInfoList[i].packageName
            startApplicationDetailsActivity(pkgName)
            cleanAppCacheFinished.set(false)
            runOnUiThread {
                binding.textView.text = String.format(Locale.getDefault(),
                    "%d / %d %s", i, pkgInfoList.size, getText(R.string.text_clean_cache_left))
            }
            delay(500L)
            waitAccessibility.block(5000L)
            delay(500L)

            // user interrupt process
            if (cleanCacheInterrupt.get()) break
        }
        cleanCacheFinished.set(true)

        runOnUiThread {
            binding.textView.text = if (cleanCacheInterrupt.get())
                    getText(R.string.text_clean_cache_interrupt)
                    else getText(R.string.text_clean_cache_finish)
            binding.btnOpenAccessibility.isEnabled = true
            binding.btnCleanUserAppCache.isEnabled = true
            binding.btnCleanSystemAppCache.isEnabled = true
            binding.btnCleanAllAppCache.isEnabled = true
        }
    }

    // method to check is the user has permitted the accessibility permission
    // if not then prompt user to the system's Settings activity
    private fun checkAccessibilityPermission(): Boolean {
        var accessEnabled = 0
        try {
            accessEnabled =
                Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

        return accessEnabled != 0
    }

    private fun getListInstalledUserApps(): List<PackageInfo> {
        val pkgInfoList = ArrayList<PackageInfo>()
        val list = packageManager.getInstalledPackages(0)
        for (i in list.indices) {
            val packageInfo = list[i]
            val flags = packageInfo!!.applicationInfo.flags
            val isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (!isSystemApp or isUpdatedSystemApp)
                pkgInfoList.add(packageInfo)
        }
        return pkgInfoList
    }

    private fun getListInstalledSystemApps(): List<PackageInfo> {
        val pkgInfoList = ArrayList<PackageInfo>()
        val list = packageManager.getInstalledPackages(0)
        for (i in list.indices) {
            val packageInfo = list[i]
            val flags = packageInfo!!.applicationInfo.flags
            val isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystemApp and !isUpdatedSystemApp)
                pkgInfoList.add(packageInfo)
        }
        return pkgInfoList
    }

    private fun startApplicationDetailsActivity(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    companion object {
        val cleanAppCacheFinished = AtomicBoolean(false)
        val cleanCacheFinished = AtomicBoolean(true)
        val cleanCacheInterrupt = AtomicBoolean(false)
        val waitAccessibility = ConditionVariable()
        private val TAG = AppCacheCleanerActivity::class.java.simpleName
    }
}