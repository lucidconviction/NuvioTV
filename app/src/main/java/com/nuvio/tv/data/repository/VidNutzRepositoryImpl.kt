package com.robbdeeze.nuviotv.data.repository

import com.robbdeeze.nuviotv.core.network.HttpClient
import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import com.robbdeeze.nuviotv.domain.model.VidNutzCategory
import com.robbdeeze.nuviotv.domain.model.VidNutzVideo
import com.robbdeeze.nuviotv.domain.repository.VidNutzRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VidNutzRepositoryImpl @Inject constructor() : VidNutzRepository {

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
        return if (category == VidNutzCategory.TRENDING) fetchTrending(page)
        else search(categoryToQuery(category), page)
    }

    override suspend fun searchVideos(query: String, page: Int): List<VidNutzVideo> {
        return search(query, page)
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
        VidNutzCategory.TRENDING -> "trending"
    }

    private suspend fun fetchTrending(page: Int): List<VidNutzVideo> = withContext(Dispatchers.IO) {
        if (page == 1) {
            val newPipeResults = PlatformYouTubeSearch.search("trending")
            if (newPipeResults.isNotEmpty()) {
                return@withContext newPipeResults.shuffled().take(20)
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
                        .shuffled().take(20).map { it.toVidNutz() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        emptyList()
    }

    private suspend fun search(query: String, page: Int): List<VidNutzVideo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        if (page == 1) {
            val newPipeResults = PlatformYouTubeSearch.search(query)
            if (newPipeResults.isNotEmpty()) {
                return@withContext newPipeResults.shuffled().take(20)
            }
        }
        val offset = page * 3
        val encodedQuery = encodeUrl(query)
        for (i in invidiousInstances.indices) {
            val idx = (instanceCounter.incrementAndGet() + i) % invidiousInstances.size
            val instance = invidiousInstances[idx]
            try {
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video&sort=relevance&page=${page + offset}"
                val response = httpGetText(url)
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    return@withContext raw.filter { it.lengthSeconds in 30..1800 }
                        .shuffled().take(20).map { it.toVidNutz() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        for (i in pipedInstances.indices) {
            val idx = (instanceCounter.incrementAndGet() + i) % pipedInstances.size
            val instance = pipedInstances[idx]
            try {
                val url = "$instance/search?q=$encodedQuery&filter=videos&page=${page + offset}"
                val response = httpGetText(url)
                val parsed = json.decodeFromString<PipedSearchResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    return@withContext parsed.items
                        .filter { it.duration in 30..1800 }
                        .shuffled().take(20).map { it.toVidNutz() }
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
                response.body.string()
            }
        }
    }
}
