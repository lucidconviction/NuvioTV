package com.nuvio.tv.data.iptv

import com.nuvio.tv.core.network.HttpClient
import com.nuvio.tv.domain.model.IptvCategory
import com.nuvio.tv.domain.model.IptvChannel
import com.nuvio.tv.domain.model.IptvEpgEntry
import com.nuvio.tv.domain.model.IptvSource
import com.nuvio.tv.domain.repository.IptvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvRepositoryImpl @Inject constructor(
    private val storage: IptvStorage
) : IptvRepository {

    override fun getSources(): Flow<List<IptvSource>> = storage.sources
    override suspend fun addSource(source: IptvSource) = storage.addSource(source)
    override suspend fun removeSource(url: String) = storage.removeSource(url)
    override fun getFavorites(): Flow<List<IptvChannel>> = storage.favorites
    override suspend fun addFavorite(channel: IptvChannel) = storage.addFavorite(channel)
    override suspend fun removeFavorite(channelId: String) = storage.removeFavorite(channelId)

    private val m3uClient = HttpClient.client.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun getChannels(source: IptvSource): List<IptvChannel> = withContext(Dispatchers.IO) {
        when (source.type.lowercase()) {
            "m3u" -> {
                val request = Request.Builder().url(source.url).build()
                try {
                    m3uClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext emptyList()
                        val body = response.body!!
                        M3uParser.parse(body.byteStream())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            "xtream" -> {
                val params = parseXtreamParams(source.url) ?: return@withContext emptyList()
                XtreamClient(params.first, params.second, params.third).getChannels()
            }
            "stalker" -> {
                StalkerClient(source.url, "00:1A:79:XX:XX:XX").getChannels()
            }
            else -> emptyList()
        }
    }

    override fun getChannelsFlow(source: IptvSource): Flow<IptvChannel> = flow {
        if (source.type.lowercase() != "m3u") {
            getChannels(source).forEach { emit(it) }
            return@flow
        }
        val request = Request.Builder().url(source.url).build()
        try {
            m3uClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@flow
                val body = response.body!!
                body.byteStream().use { stream ->
                    M3uParser.parseFlow(stream).collect { emit(it) }
                }
            }
        } catch (_: Exception) { }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCategories(source: IptvSource): List<IptvCategory> = withContext(Dispatchers.IO) {
        when (source.type.lowercase()) {
            "xtream" -> {
                val params = parseXtreamParams(source.url) ?: return@withContext emptyList()
                XtreamClient(params.first, params.second, params.third).getCategories()
            }
            else -> emptyList()
        }
    }

    override suspend fun getEpg(source: IptvSource): Map<String, List<IptvEpgEntry>> = withContext(Dispatchers.IO) {
        emptyMap()
    }

    private fun parseXtreamParams(url: String): Triple<String, String, String>? {
        return try {
            val uri = URI(url)
            val query = uri.query ?: return null
            val params = query.split("&").associate {
                val parts = it.split("=")
                parts[0] to parts.getOrElse(1) { "" }
            }
            val username = params["username"] ?: return null
            val password = params["password"] ?: return null
            val baseUrl = "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
            Triple(baseUrl, username, password)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
