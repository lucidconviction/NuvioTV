package com.nuvio.tv.domain.repository

import com.nuvio.tv.domain.model.IptvChannel
import com.nuvio.tv.domain.model.IptvCategory
import com.nuvio.tv.domain.model.IptvEpgEntry
import com.nuvio.tv.domain.model.IptvSource
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
