package com.github.bmx666.appcachecleaner.data

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import com.github.bmx666.appcachecleaner.util.getInternalCacheSize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// Injectable boundary over PackageManager / StorageStatsManager. Callers (repository,
// package-list ViewModel) depend on this interface so a fake can stand in for the
// framework in tests. It deals only in plain types (Long cache size, not StorageStats) so
// everything downstream stays free of Android storage classes and pre-O guards.
interface PackageSource {
    suspend fun getInstalledApps(
        systemNotUpdated: Boolean,
        systemUpdated: Boolean,
        userOnly: Boolean,
    ): List<PackageInfo>

    suspend fun getDisabledApps(): List<PackageInfo>

    // Internal cache size in bytes; 0 on pre-O (no StorageStats API) or on any error.
    suspend fun getCacheBytes(pkgInfo: PackageInfo): Long

    suspend fun getApplicationLabel(pkgInfo: PackageInfo): String
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

    override suspend fun getCacheBytes(pkgInfo: PackageInfo): Long {
        // Inline SDK gate (not SdkProvider) so lint can prove the @RequiresApi(O)
        // getStorageStats/getInternalCacheSize calls below are reached only on O+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0L
        val stats = PackageManagerHelper.getStorageStats(context, pkgInfo) ?: return 0L
        return stats.getInternalCacheSize()
    }

    override suspend fun getApplicationLabel(pkgInfo: PackageInfo): String =
        PackageManagerHelper.getApplicationLabel(context, pkgInfo)
}
