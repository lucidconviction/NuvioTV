package com.robbdeeze.nuviotv.di

import com.robbdeeze.nuviotv.core.auth.AuthManager
import com.robbdeeze.nuviotv.core.plugin.PluginManager
import com.robbdeeze.nuviotv.core.plugin.PluginRuntime
import com.robbdeeze.nuviotv.core.plugin.cloudstream.ExternalExtensionLoader
import com.robbdeeze.nuviotv.core.plugin.cloudstream.ExternalExtensionRunner
import com.robbdeeze.nuviotv.core.plugin.cloudstream.ExternalRepoParser
import com.robbdeeze.nuviotv.core.sync.PluginSyncService
import com.robbdeeze.nuviotv.data.local.PluginDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PluginModule {

    @Provides
    @Singleton
    fun providePluginRuntime(): PluginRuntime {
        return PluginRuntime()
    }

    @Provides
    @Singleton
    fun providePluginManager(
        dataStore: PluginDataStore,
        runtime: PluginRuntime,
        pluginSyncService: PluginSyncService,
        authManager: AuthManager,
        externalRepoParser: ExternalRepoParser,
        externalExtensionLoader: ExternalExtensionLoader,
        externalExtensionRunner: ExternalExtensionRunner
    ): PluginManager {
        return PluginManager(
            dataStore, runtime, pluginSyncService, authManager,
            externalRepoParser, externalExtensionLoader, externalExtensionRunner
        )
    }
}
