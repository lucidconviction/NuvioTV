package com.robbdeeze.nuviotv.data.iptv

import com.robbdeeze.nuviotv.core.network.HttpClient
import com.robbdeeze.nuviotv.domain.model.IptvCategory
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.IptvEpgEntry
import com.robbdeeze.nuviotv.domain.model.IptvSource
import com.robbdeeze.nuviotv.domain.repository.IptvRepository
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
    private val storage: IptvStorage,
    private val channelCache: ChannelCache
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
        val cacheKey = source.url
        val cached = channelCache.get(cacheKey)
        if (cached != null) return@withContext cached
        val channels = when (source.type.lowercase()) {
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
                val (portalUrl, mac) = parseStalkerParams(source.url)
                StalkerClient(portalUrl, mac).getChannels()
            }
            else -> emptyList()
        }
        channelCache.put(cacheKey, channels)
        channels
    }

    override fun getChannelsFlow(source: IptvSource): Flow<IptvChannel> = flow {
        val cacheKey = source.url
        val cached = channelCache.get(cacheKey)
        if (cached != null) {
            cached.forEach { emit(it) }
            return@flow
        }
        if (source.type.lowercase() != "m3u") {
            val channels = getChannels(source)
            channels.forEach { emit(it) }
            channelCache.put(cacheKey, channels)
            return@flow
        }
        val request = Request.Builder().url(source.url).build()
        try {
            val fetched = mutableListOf<IptvChannel>()
            m3uClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@flow
                val body = response.body!!
                body.byteStream().use { stream ->
                    M3uParser.parseFlow(stream).collect { ch ->
                        fetched.add(ch)
                        emit(ch)
                    }
                }
            }
            channelCache.put(cacheKey, fetched)
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
        when (source.type.lowercase()) {
            "xtream" -> {
                val params = parseXtreamParams(source.url) ?: return@withContext emptyMap()
                XtreamClient(params.first, params.second, params.third).getEpgXmltv()
            }
            "stalker" -> {
                val (portalUrl, mac) = parseStalkerParams(source.url)
                StalkerClient(portalUrl, mac).getEpg()
            }
            "m3u" -> {
                val epgUrl = source.epgUrl ?: return@withContext emptyMap()
                try {
                    val request = Request.Builder().url(epgUrl).build()
                    m3uClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext emptyMap()
                        parseXmltv(response.body!!.byteStream())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyMap()
                }
            }
            else -> emptyMap()
        }
    }

    override suspend fun getVod(source: IptvSource): List<com.robbdeeze.nuviotv.domain.model.IptvVodItem> = withContext(Dispatchers.IO) {
        when (source.type.lowercase()) {
            "xtream" -> {
                val params = parseXtreamParams(source.url) ?: return@withContext emptyList()
                XtreamClient(params.first, params.second, params.third).getVod()
            }
            else -> emptyList()
        }
    }

    override suspend fun getSeries(source: IptvSource): List<com.robbdeeze.nuviotv.domain.model.IptvSeries> = withContext(Dispatchers.IO) {
        when (source.type.lowercase()) {
            "xtream" -> {
                val params = parseXtreamParams(source.url) ?: return@withContext emptyList()
                XtreamClient(params.first, params.second, params.third).getSeries()
            }
            else -> emptyList()
        }
    }

    override suspend fun getSeriesInfo(source: IptvSource, seriesId: String): com.robbdeeze.nuviotv.domain.model.IptvSeries? = withContext(Dispatchers.IO) {
        when (source.type.lowercase()) {
            "xtream" -> {
                val params = parseXtreamParams(source.url) ?: return@withContext null
                XtreamClient(params.first, params.second, params.third).getSeriesInfo(seriesId)
            }
            else -> null
        }
    }

    private fun parseXmltv(stream: java.io.InputStream): Map<String, List<IptvEpgEntry>> {
        val epgMap = mutableMapOf<String, MutableList<IptvEpgEntry>>()
        try {
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(stream, "UTF-8")

            var channelId: String? = null
            var currentTitle: String? = null
            var currentStart: Long = 0
            var currentStop: Long = 0
            var currentDesc: String? = null

            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                channelId = parser.getAttributeValue(null, "channel")
                                val startStr = parser.getAttributeValue(null, "start")
                                val stopStr = parser.getAttributeValue(null, "stop")
                                currentStart = parseXmltvTime(startStr)
                                currentStop = parseXmltvTime(stopStr)
                            }
                            "title" -> currentTitle = parser.nextText()
                            "desc" -> currentDesc = parser.nextText()
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name == "programme" && channelId != null && currentTitle != null) {
                            epgMap.getOrPut(channelId) { mutableListOf() }.add(
                                IptvEpgEntry(
                                    title = currentTitle,
                                    description = currentDesc,
                                    startTimeMs = currentStart,
                                    endTimeMs = currentStop
                                )
                            )
                        }
                        channelId = null
                        currentTitle = null
                        currentDesc = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return epgMap
    }

    private fun parseXmltvTime(timeStr: String?): Long {
        if (timeStr.isNullOrBlank()) return 0
        return try {
            val format = java.text.SimpleDateFormat("yyyyMMddHHmmss Z", java.util.Locale.US)
            format.parse(timeStr)?.time ?: 0
        } catch (e: Exception) {
            0
        }
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

    private fun parseStalkerParams(url: String): Pair<String, String> {
        val pipeIdx = url.indexOf('|')
        val mac = if (pipeIdx >= 0) {
            val after = url.substring(pipeIdx + 1)
            if (after.startsWith("mac=")) after.substring(4) else after
        } else ""
        val portalUrl = if (pipeIdx >= 0) url.substring(0, pipeIdx) else url
        return Pair(portalUrl.trim().trimEnd('/'), mac.ifBlank { "00:1A:79:00:00:00" })
    }
}
