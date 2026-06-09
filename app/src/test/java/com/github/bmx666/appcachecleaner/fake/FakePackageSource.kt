package com.github.bmx666.appcachecleaner.fake

import android.content.pm.PackageInfo
import com.github.bmx666.appcachecleaner.data.PackageSource

// In-memory PackageSource for repository/ViewModel tests. cacheBytes is mutable so a test
// can simulate a post-clean drop before calling refreshStatsAfterCacheClean.
class FakePackageSource(
    var installed: List<PackageInfo> = emptyList(),
    var disabled: List<PackageInfo> = emptyList(),
    val cacheBytes: MutableMap<String, Long> = mutableMapOf(),
    val labels: MutableMap<String, String> = mutableMapOf(),
) : PackageSource {

    override suspend fun getInstalledApps(
        systemNotUpdated: Boolean,
        systemUpdated: Boolean,
        userOnly: Boolean,
    ): List<PackageInfo> = installed

    override suspend fun getDisabledApps(): List<PackageInfo> = disabled

    override suspend fun getCacheBytes(pkgInfo: PackageInfo): Long =
        cacheBytes[pkgInfo.packageName] ?: 0L

    override suspend fun getApplicationLabel(pkgInfo: PackageInfo): String =
        labels[pkgInfo.packageName] ?: pkgInfo.packageName
}
