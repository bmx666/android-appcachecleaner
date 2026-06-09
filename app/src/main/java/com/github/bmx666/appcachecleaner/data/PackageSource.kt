package com.github.bmx666.appcachecleaner.data

import android.app.usage.StorageStats
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// Injectable boundary over PackageManager / StorageStatsManager. Callers (repository,
// package-list ViewModel) depend on this interface so a fake can stand in for the
// framework in tests; the production impl just delegates to PackageManagerHelper.
interface PackageSource {
    suspend fun getInstalledApps(
        systemNotUpdated: Boolean,
        systemUpdated: Boolean,
        userOnly: Boolean,
    ): List<PackageInfo>

    suspend fun getDisabledApps(): List<PackageInfo>

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStorageStats(pkgInfo: PackageInfo): StorageStats?

    suspend fun getApplicationLabel(pkgInfo: PackageInfo): String

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCacheSizeDiff(old: StorageStats?, new: StorageStats?): Long
}

class AndroidPackageSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PackageSource {

    override suspend fun getInstalledApps(
        systemNotUpdated: Boolean,
        systemUpdated: Boolean,
        userOnly: Boolean,
    ): List<PackageInfo> =
        PackageManagerHelper.getInstalledApps(context, systemNotUpdated, systemUpdated, userOnly)

    override suspend fun getDisabledApps(): List<PackageInfo> =
        PackageManagerHelper.getDisabledApps(context)

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getStorageStats(pkgInfo: PackageInfo): StorageStats? =
        PackageManagerHelper.getStorageStats(context, pkgInfo)

    override suspend fun getApplicationLabel(pkgInfo: PackageInfo): String =
        PackageManagerHelper.getApplicationLabel(context, pkgInfo)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getCacheSizeDiff(old: StorageStats?, new: StorageStats?): Long =
        PackageManagerHelper.getCacheSizeDiff(old, new)
}
