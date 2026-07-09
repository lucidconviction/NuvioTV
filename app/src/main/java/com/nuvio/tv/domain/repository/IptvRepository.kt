package com.robbdeeze.nuviotv.domain.repository

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.IptvCategory
import com.robbdeeze.nuviotv.domain.model.IptvEpgEntry
import com.robbdeeze.nuviotv.domain.model.IptvSource
import kotlinx.coroutines.flow.Flow

interface IptvRepository {
    fun getSources(): Flow<List<IptvSource>>
    suspend fun addSource(source: IptvSource)
    suspend fun removeSource(url: String)
    fun getFavorites(): Flow<List<IptvChannel>>
    suspend fun addFavorite(channel: IptvChannel)
    suspend fun removeFavorite(channelId: String)
    suspend fun getChannels(source: IptvSource): List<IptvChannel>
    fun getChannelsFlow(source: IptvSource): Flow<IptvChannel>
    suspend fun getCategories(source: IptvSource): List<IptvCategory>
    suspend fun getEpg(source: IptvSource): Map<String, List<IptvEpgEntry>>
}
