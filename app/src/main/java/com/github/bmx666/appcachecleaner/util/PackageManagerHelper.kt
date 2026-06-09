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
import androidx.annotation.RequiresApi
import com.github.bmx666.appcachecleaner.log.Logger


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
                val flags = packageInfo!!.applicationInfo?.flags ?: 0
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
        suspend fun getDisabledApps(context: Context): ArrayList<PackageInfo> {
            val list = context.packageManager.getInstalledPackages(0)
            val pkgInfoList = ArrayList<PackageInfo>()
            for (i in list.indices) {
                val packageInfo = list[i]
                if (packageInfo?.applicationInfo?.enabled == false)
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
        suspend fun getApplicationIcon(context: Context, pkgName: String): Drawable? {
            return try {
                context.packageManager.getApplicationIcon(pkgName)
            } catch (e: PackageManager.NameNotFoundException) {
                Logger.e(e.message ?: e.toString())
                null
            }
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
                            Logger.e(e.message ?: e.toString())
                        }
                } catch (e: PackageManager.NameNotFoundException) {
                    Logger.e(e.message ?: e.toString())
                }
            }

            return null
        }

        @JvmStatic
        suspend fun getApplicationLabel(context: Context, pkgInfo: PackageInfo): String {
            var localizedLabel: String? = null
            context.packageManager?.let { pm ->
                try {
                    pkgInfo.applicationInfo?.let { applicationInfo ->
                        val res = pm.getResourcesForApplication(applicationInfo)
                        val resId = applicationInfo.labelRes
                        if (resId != 0)
                            try {
                                localizedLabel = res.getString(resId)
                            } catch (e: Resources.NotFoundException) {
                                Logger.e(e.message ?: e.toString())
                            }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Logger.e(e.message ?: e.toString())
                }
            }

            return localizedLabel
                ?: pkgInfo.applicationInfo?.nonLocalizedLabel?.toString()
                ?: pkgInfo.packageName
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getStorageStats(context: Context, pkgInfo: PackageInfo): StorageStats? {
            try {
                val storageStatsManager =
                    context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                return storageStatsManager.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT, pkgInfo.packageName,
                    android.os.Process.myUserHandle()
                )
            } catch (e: Exception) {
                Logger.e(e.message ?: e.toString())
            }

            return null
        }
    }
}