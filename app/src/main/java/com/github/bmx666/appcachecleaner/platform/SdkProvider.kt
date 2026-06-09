package com.github.bmx666.appcachecleaner.platform

import android.os.Build
import javax.inject.Inject

// Indirection over Build.VERSION.SDK_INT so version-gated branches (StorageStats on O+,
// recomposition de-dup on U+, etc.) can be exercised from both sides in plain JVM tests.
interface SdkProvider {
    val sdkInt: Int
}

class DefaultSdkProvider @Inject constructor() : SdkProvider {
    override val sdkInt: Int get() = Build.VERSION.SDK_INT
}

// Named helpers keep call sites reading like the old inline `SDK_INT >= O` checks.
fun SdkProvider.atLeastO(): Boolean = sdkInt >= Build.VERSION_CODES.O
fun SdkProvider.atLeastN(): Boolean = sdkInt >= Build.VERSION_CODES.N
fun SdkProvider.atLeastU(): Boolean = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
