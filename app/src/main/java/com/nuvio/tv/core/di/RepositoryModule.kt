package com.robbdeeze.nuviotv.core.di

import com.robbdeeze.nuviotv.data.repository.AddonRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.CatalogRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.LibraryRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.MetaRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.StreamRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.SubtitleRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.SyncRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.WatchProgressRepositoryImpl
import com.robbdeeze.nuviotv.domain.repository.AddonRepository
import com.robbdeeze.nuviotv.domain.repository.CatalogRepository
import com.robbdeeze.nuviotv.domain.repository.LibraryRepository
import com.robbdeeze.nuviotv.domain.repository.MetaRepository
import com.robbdeeze.nuviotv.domain.repository.StreamRepository
import com.robbdeeze.nuviotv.domain.repository.SubtitleRepository
import com.robbdeeze.nuviotv.domain.repository.SyncRepository
import com.robbdeeze.nuviotv.domain.repository.WatchProgressRepository
import com.robbdeeze.nuviotv.domain.repository.IptvRepository
import com.robbdeeze.nuviotv.domain.repository.VidNutzRepository
import com.robbdeeze.nuviotv.domain.repository.MusicNutzRepository
import com.robbdeeze.nuviotv.data.iptv.IptvRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.VidNutzRepositoryImpl
import com.robbdeeze.nuviotv.data.repository.MusicNutzRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAddonRepository(impl: AddonRepositoryImpl): AddonRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindMetaRepository(impl: MetaRepositoryImpl): MetaRepository

    @Binds
    @Singleton
    abstract fun bindStreamRepository(impl: StreamRepositoryImpl): StreamRepository

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(impl: SubtitleRepositoryImpl): SubtitleRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindWatchProgressRepository(impl: WatchProgressRepositoryImpl): WatchProgressRepository

    @Binds
    @Singleton
    abstract fun bindIptvRepository(impl: IptvRepositoryImpl): IptvRepository

    @Binds
    @Singleton
    abstract fun bindVidNutzRepository(impl: VidNutzRepositoryImpl): VidNutzRepository

    @Binds
    @Singleton
    abstract fun bindMusicNutzRepository(impl: MusicNutzRepositoryImpl): MusicNutzRepository
}
