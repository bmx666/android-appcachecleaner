package com.github.bmx666.appcachecleaner.di

import com.github.bmx666.appcachecleaner.platform.DefaultDispatcherProvider
import com.github.bmx666.appcachecleaner.platform.DefaultSdkProvider
import com.github.bmx666.appcachecleaner.platform.DispatcherProvider
import com.github.bmx666.appcachecleaner.platform.SdkProvider
import com.github.bmx666.appcachecleaner.data.AndroidPackageSource
import com.github.bmx666.appcachecleaner.data.PackageSource
import com.github.bmx666.appcachecleaner.util.DefaultEventBus
import com.github.bmx666.appcachecleaner.util.EventBus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Binds the cross-cutting platform/seam interfaces to their production implementations.
// Tests provide fakes directly (no Hilt) or via @TestInstallIn replacements.
@Module
@InstallIn(SingletonComponent::class)
abstract class SeamModule {

    @Binds
    @Singleton
    abstract fun bindSdkProvider(impl: DefaultSdkProvider): SdkProvider

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindEventBus(impl: DefaultEventBus): EventBus

    @Binds
    abstract fun bindPackageSource(impl: AndroidPackageSource): PackageSource
}
