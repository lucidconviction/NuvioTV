package com.robbdeeze.nuviotv.data.repository

import com.robbdeeze.nuviotv.core.network.HttpClient
import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import com.robbdeeze.nuviotv.data.youtube.VideoSuggestionEngine
import com.robbdeeze.nuviotv.domain.model.VidNutzCategory
import com.robbdeeze.nuviotv.domain.model.VidNutzVideo
import com.robbdeeze.nuviotv.domain.repository.VidNutzRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VidNutzRepositoryImpl @Inject constructor() : VidNutzRepository {

    private val preCache = ConcurrentHashMap<String, List<VidNutzVideo>>()
    private val preCacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val invidiousInstances = listOf(
        "https://inv.nadeko.net",
        "https://vid.puffyan.us",
        "https://yewtu.be",
        "https://inv.skyn3t.in",
        "https://invidious.snopyta.org",
    )

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.lunar.icu",
        "https://piped-api.garudalinux.org",
    )

    private val instanceCounter = AtomicInteger(0)

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getVideosByCategory(category: VidNutzCategory, page: Int): List<VidNutzVideo> {
        val cacheKey = "cat:${category.name}:$page"
        val cached = preCache[cacheKey]
        if (page > 1 && cached != null) {
            preCache.remove(cacheKey)
            return cached
        }
        val results = if (category == VidNutzCategory.TRENDING) fetchTrending(page)
        else if (category == VidNutzCategory.LIVE_STREAMS) search("live streams now", page, category.displayName)
        else search(categoryToQuery(category), page, category.displayName)
        if (page == 1 && results.isNotEmpty()) {
            val nextKey = "cat:${category.name}:${page + 1}"
            preCacheScope.launch {
                val next = if (category == VidNutzCategory.TRENDING) fetchTrending(page + 1)
                else if (category == VidNutzCategory.LIVE_STREAMS) search("live streams now", page + 1, category.displayName)
                else search(categoryToQuery(category), page + 1, category.displayName)
                preCache[nextKey] = next
            }
        }
        return results
    }

    override suspend fun searchVideos(query: String, page: Int): List<VidNutzVideo> {
        val cacheKey = "search:$query:$page"
        if (page > 1) {
            preCache.remove(cacheKey)?.let { return it }
        }
        val results = search(query, page)
        if (page == 1 && results.isNotEmpty()) {
            val nextKey = "search:$query:${page + 1}"
            preCacheScope.launch {
                val next = search(query, page + 1)
                preCache[nextKey] = next
            }
        }
        return results
    }

    private fun categoryToQuery(category: VidNutzCategory): String = when (category) {
        VidNutzCategory.POLITICS -> "politics news today"
        VidNutzCategory.NEWS -> "breaking news today"
        VidNutzCategory.MUSIC -> "music videos"
        VidNutzCategory.SPORTS -> "sports highlights"
        VidNutzCategory.DOCUMENTARY -> "documentary"
        VidNutzCategory.TECHNOLOGY -> "technology tech review"
        VidNutzCategory.ENTERTAINMENT -> "entertainment"
        VidNutzCategory.COMEDY -> "comedy standup"
        VidNutzCategory.SCIENCE -> "science"
        VidNutzCategory.TRUE_CRIME -> "true crime documentary"
        VidNutzCategory.FOOD_DRINK -> "food drink cooking"
        VidNutzCategory.LIVE_STREAMS -> "live"
        VidNutzCategory.TRENDING -> "trending"
    }

    private suspend fun fetchTrending(page: Int): List<VidNutzVideo> = withContext(Dispatchers.IO) {
        if (page == 1) {
            val engineResults = VideoSuggestionEngine.suggest("trending", "Trending", 48)
            if (engineResults.isNotEmpty()) {
                return@withContext engineResults.shuffled().take(15)
            }
        }
        val offset = page * 3
        for (i in invidiousInstances.indices) {
            val idx = (instanceCounter.incrementAndGet() + i) % invidiousInstances.size
            val instance = invidiousInstances[idx]
            try {
                val url = "$instance/api/v1/trending?type=video&page=${page + offset}"
                val response = httpGetText(url)
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    return@withContext raw.filter { it.lengthSeconds in 30..1800 }
                        .shuffled().take(15).map { it.toVidNutz() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        emptyList()
    }

    private suspend fun search(query: String, page: Int, categoryKey: String? = null): List<VidNutzVideo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        // Page 1: try local engines first (more reliable than Invidious/Piped)
        if (page == 1) {
            val engineResults = if (categoryKey != null) {
                VideoSuggestionEngine.suggest(query, categoryKey, 48)
            } else {
                PlatformYouTubeSearch.search(query)
            }
            if (engineResults.isNotEmpty()) {
                return@withContext engineResults.shuffled().take(15)
            }
        }
        val encodedQuery = encodeUrl(query)
        for (i in invidiousInstances.indices) {
            val idx = (instanceCounter.incrementAndGet() + i) % invidiousInstances.size
            val instance = invidiousInstances[idx]
            try {
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video&sort=relevance&page=$page"
                val response = httpGetText(url)
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    val mapped = raw.filter { it.lengthSeconds in 30..1800 }
                        .shuffled().map { it.toVidNutz() }
                    val fromIndex = ((page - 1) * 15).coerceAtMost(mapped.size)
                    val toIndex = (fromIndex + 15).coerceAtMost(mapped.size)
                    return@withContext mapped.subList(fromIndex, toIndex)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        for (i in pipedInstances.indices) {
            val idx = (instanceCounter.incrementAndGet() + i) % pipedInstances.size
            val instance = pipedInstances[idx]
            try {
                val url = "$instance/search?q=$encodedQuery&filter=videos&page=$page"
                val response = httpGetText(url)
                val parsed = json.decodeFromString<PipedSearchResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    val mapped = parsed.items.filter { it.duration in 30..1800 }
                        .shuffled().map { it.toVidNutz() }
                    val fromIndex = ((page - 1) * 15).coerceAtMost(mapped.size)
                    val toIndex = (fromIndex + 15).coerceAtMost(mapped.size)
                    return@withContext mapped.subList(fromIndex, toIndex)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        emptyList()
    }

    private fun encodeUrl(s: String): String {
        return s.replace(" ", "+").replace(",", "%2C").replace(":", "%3A")
            .replace("&", "%26").replace("?", "%3F").replace("#", "%23")
    }

    @Serializable
    data class InvidiousVideo(
        val title: String = "", val videoId: String = "", val author: String = "",
        val lengthSeconds: Int = 0, val viewCount: Long = 0, val publishedText: String = "",
    )

    private fun InvidiousVideo.toVidNutz() = VidNutzVideo(
        videoId = videoId, title = title,
        thumbnailUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
        channelName = author, durationSeconds = lengthSeconds,
        viewCount = viewCount, uploadDate = publishedText,
    )

    @Serializable
    data class PipedSearchItem(
        val url: String = "", val title: String = "", val uploader: String = "",
        val duration: Long = 0, val views: Long = 0, val uploadedDate: String = "",
    )

    @Serializable
    data class PipedSearchResponse(val items: List<PipedSearchItem> = emptyList())

    private fun PipedSearchItem.toVidNutz(): VidNutzVideo {
        val videoId = url.removePrefix("/watch?v=")
        return VidNutzVideo(
            videoId = videoId, title = title,
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
            channelName = uploader, durationSeconds = duration.toInt(),
            viewCount = views, uploadDate = uploadedDate,
        )
    }

    companion object {
        private suspend fun httpGetText(url: String): String = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                response.body!!.string()
            }
        }
    }
}
