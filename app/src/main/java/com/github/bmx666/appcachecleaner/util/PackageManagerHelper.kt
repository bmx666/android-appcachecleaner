package com.github.bmx666.appcachecleaner.util

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.storage.StorageManager


class PackageManagerHelper {

    companion object {

        @JvmStatic
        suspend fun getInstalledApps(context: Context,
                             systemNotUpdated: Boolean,
                             systemUpdated: Boolean,
                             userOnly: Boolean): ArrayList<PackageInfo> {
            val list = context.packageManager.getInstalledPackages(0)
            val pkgInfoList = ArrayList<PackageInfo>()
            for (i in list.indices) {
                val packageInfo = list[i]
                val flags = packageInfo!!.applicationInfo.flags
                val isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val addPkg = (systemNotUpdated && (isSystemApp and !isUpdatedSystemApp)) or
                        (systemUpdated && (isSystemApp and isUpdatedSystemApp)) or
                        (userOnly && (!isSystemApp and !isUpdatedSystemApp))
                if (addPkg)
                    pkgInfoList.add(packageInfo)
            }

            return pkgInfoList
        }

        @JvmStatic
        suspend fun getCustomInstalledApps(context: Context,
                                   pkgList: Set<String>): ArrayList<PackageInfo> {
            val list = context.packageManager.getInstalledPackages(0)
            val pkgInfoList = ArrayList<PackageInfo>()
            for (i in list.indices) {
                val packageInfo = list[i]
                if (pkgList.contains(packageInfo.packageName))
                    pkgInfoList.add(packageInfo)
            }

            return pkgInfoList
        }

        @JvmStatic
        suspend fun getApplicationIcon(context: Context, pkgInfo: PackageInfo): Drawable? {
            return context.packageManager.getApplicationIcon(pkgInfo.packageName)
        }

        @JvmStatic
        suspend fun getApplicationResourceString(context: Context, pkgName: String,
                                         resourceName: String): String? {
            context.packageManager?.let { pm ->
                try {
                    val res = pm.getResourcesForApplication(pkgName)
                    val resId = res.getIdentifier(resourceName,"string", pkgName)
                    if (resId != 0)
                        try {
                            return res.getString(resId)
                        } catch (e: Resources.NotFoundException) {
                            e.printStackTrace()
                        }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }

            return null
        }

        @JvmStatic
        suspend fun getApplicationLabel(context: Context, pkgInfo: PackageInfo): String {
            var localizedLabel: String? = null
            context.packageManager?.let { pm ->
                try {
                    val res = pm.getResourcesForApplication(pkgInfo.applicationInfo)
                    val resId = pkgInfo.applicationInfo.labelRes
                    if (resId != 0)
                        try {
                            localizedLabel = res.getString(resId)
                        } catch (e: Resources.NotFoundException) {
                            e.printStackTrace()
                        }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }

            return localizedLabel
                ?: pkgInfo.applicationInfo.nonLocalizedLabel?.toString()
                ?: pkgInfo.packageName
        }

        @JvmStatic
        suspend fun getStorageStats(context: Context, pkgInfo: PackageInfo): StorageStats? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

            try {
                val storageStatsManager =
                    context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                return storageStatsManager.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT, pkgInfo.packageName,
                    android.os.Process.myUserHandle()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

        @JvmStatic
        fun getCacheSizeDiff(old: StorageStats?, new: StorageStats?): Long {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0
            if (old == null || new == null) return 0

            try {
                return if (new.cacheBytes >= old.cacheBytes) 0
                    else old.cacheBytes - new.cacheBytes
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return 0
        }
    }
}