package com.robbdeeze.nuviotv.data.sports

import android.util.Log
import com.google.gson.Gson
import com.robbdeeze.nuviotv.core.network.HttpClient
import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import com.robbdeeze.nuviotv.domain.model.YoutubeQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.ServiceList
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object YouTubeStreamResolver {

    private const val TAG = "YouTubeStreamResolver"
    private const val FALLBACK_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val TIMEOUT_MS = 20_000L
    private const val CONFIG_TTL_MS = 3 * 60 * 60 * 1000L
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val PREFERRED_CLIENT = "android_vr"

    private val gson = Gson()
    private val json = Json { ignoreUnknownKeys = true }

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.lunar.icu",
        "https://piped-api.garudalinux.org",
    )

    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    private data class CachedConfig(
        val apiKey: String,
        val visitorData: String?,
        val fetchedAt: Long = System.currentTimeMillis()
    )
    private val cachedConfig = AtomicReference<CachedConfig?>(null)
    private val resultCache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val result: YoutubeStreamResult,
        val fetchedAt: Long = System.currentTimeMillis()
    )

    private data class YouTubeClient(
        val name: String,
        val context: Map<String, Any>,
        val userAgent: String,
    )

    private val clients = listOf(
        YouTubeClient(
            name = "android_vr",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 12; en_US; Quest 3) gzip",
            context = mapOf(
                "clientName" to "ANDROID_VR", "clientVersion" to "1.56.21",
                "deviceMake" to "Oculus", "deviceModel" to "Quest 3",
                "osName" to "Android", "osVersion" to "12",
                "platform" to "MOBILE", "androidSdkVersion" to 32,
                "hl" to "en", "gl" to "US",
            ),
        ),
        YouTubeClient(
            name = "android",
            userAgent = "com.google.android.youtube/20.10.35 (Linux; U; Android 14; en_US) gzip",
            context = mapOf(
                "clientName" to "ANDROID", "clientVersion" to "20.10.35",
                "osName" to "Android", "osVersion" to "14",
                "platform" to "MOBILE", "androidSdkVersion" to 34,
                "hl" to "en", "gl" to "US",
            ),
        ),
        YouTubeClient(
            name = "ios",
            userAgent = "com.google.ios.youtube/20.10.1 (iPhone16,2; U; CPU iOS 17_4 like Mac OS X)",
            context = mapOf(
                "clientName" to "IOS", "clientVersion" to "20.10.1",
                "deviceModel" to "iPhone16,2", "osName" to "iPhone", "osVersion" to "17.4.0.21E219",
                "platform" to "MOBILE", "hl" to "en", "gl" to "US",
            ),
        ),
    )

    private val API_KEY_REGEX = Regex("\"INNERTUBE_API_KEY\":\"([^\"]+)\"")
    private val VISITOR_DATA_REGEX = Regex("\"VISITOR_DATA\":\"([^\"]+)\"")

    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        resolveStreamResult(videoId)?.videoUrl
    }

    suspend fun resolveStreamResult(videoId: String): YoutubeStreamResult? = withContext(Dispatchers.IO) {
        if (videoId.isBlank() || !videoId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return@withContext null

        val cached = resultCache[videoId]
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) {
            Log.d(TAG, "Cache hit for $videoId")
            return@withContext cached.result
        }

        val finalResult = try {
            withTimeout(TIMEOUT_MS) { resolveInnerTube(videoId, forceRefresh = false) }
        } catch (_: Exception) { null } ?: try {
            withTimeout(TIMEOUT_MS) { resolveInnerTube(videoId, forceRefresh = true) }
        } catch (_: Exception) { null } ?: try {
            withTimeout(TIMEOUT_MS) { resolvePiped(videoId) }
        } catch (_: Exception) { null } ?: try {
            withTimeout(TIMEOUT_MS) { resolveNewPipe(videoId) }
        } catch (_: Exception) { null }

        if (finalResult != null) {
            resultCache[videoId] = CacheEntry(finalResult)
        }
        finalResult
    }

    private fun getWatchConfig(): CachedConfig {
        val current = cachedConfig.get()
        if (current != null && System.currentTimeMillis() - current.fetchedAt < CONFIG_TTL_MS) {
            return current
        }

        return try {
            val watchUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&hl=en"
            val request = Request.Builder().url(watchUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("accept-language", "en-US,en;q=0.9")
                .build()
            val response = HttpClient.client.newCall(request).execute()
            val html = response.body?.string().orEmpty()

            val apiKey = API_KEY_REGEX.find(html)?.groupValues?.getOrNull(1) ?: FALLBACK_API_KEY
            val visitorData = VISITOR_DATA_REGEX.find(html)?.groupValues?.getOrNull(1)

            val config = CachedConfig(apiKey = apiKey, visitorData = visitorData)
            cachedConfig.set(config)
            Log.d(TAG, "Watch config fetched: apiKey=${apiKey.take(10)}... visitor=${!visitorData.isNullOrBlank()}")
            config
        } catch (e: Exception) {
            Log.w(TAG, "Watch config fetch failed: ${e.message}")
            current ?: CachedConfig(apiKey = FALLBACK_API_KEY, visitorData = null)
        }
    }

    private fun invalidateConfig() {
        cachedConfig.set(null)
    }

    private fun resolveInnerTube(videoId: String, forceRefresh: Boolean): YoutubeStreamResult? {
        if (forceRefresh) invalidateConfig()
        val config = getWatchConfig()

        val allVideo = mutableListOf<Pair<Int, Map<*, *>>>()
        val allAudio = mutableListOf<Map<*, *>>()
        var hasLoginRequired = false

        for (client in clients) {
            try {
                val payload = gson.toJson(mapOf(
                    "videoId" to videoId,
                    "contentCheckOk" to true,
                    "racyCheckOk" to true,
                    "context" to mapOf("client" to client.context),
                    "playbackContext" to mapOf(
                        "contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS")
                    ),
                ))

                val endpoint = "https://www.youtube.com/youtubei/v1/player?key=${config.apiKey}"
                val request = Request.Builder()
                    .url(endpoint)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", client.userAgent)
                    .header("accept-language", "en-US,en;q=0.9")
                    .header("origin", "https://www.youtube.com")
                    .apply {
                        val vd = config.visitorData
                        if (!vd.isNullOrBlank()) addHeader("x-goog-visitor-id", vd)
                    }
                    .post(payload.toRequestBody())
                    .build()

                val response = HttpClient.client.newCall(request).execute()
                if (!response.isSuccessful) continue
                val body = response.body!!.string()
                @Suppress("UNCHECKED_CAST")
                val parsed = gson.fromJson(body, Map::class.java) as? Map<*, *> ?: continue

                val playabilityStatus = parsed["playabilityStatus"] as? Map<*, *>
                val status = playabilityStatus?.get("status")?.toString()
                if (status == "LOGIN_REQUIRED") { hasLoginRequired = true; continue }
                if (status != null && status != "OK") continue

                val streamingData = parsed["streamingData"] as? Map<*, *> ?: continue

                for (fmt in streamingData["adaptiveFormats"] as? List<*> ?: emptyList<Any>()) {
                    val f = fmt as? Map<*, *> ?: continue
                    val mime = f["mimeType"]?.toString().orEmpty()
                    val url = f["url"]?.toString() ?: continue
                    if (url.isBlank()) continue
                    if (mime.contains("video/")) {
                        val height = (f["height"] as? Number)?.toInt()
                            ?: parseQualityLabel(f["qualityLabel"]?.toString())
                            ?: 0
                        allVideo.add(height to f)
                    } else if (mime.contains("audio/")) {
                        allAudio.add(f)
                    }
                }

                if (allVideo.isNotEmpty()) break
            } catch (_: Exception) {}
        }

        if (allVideo.isEmpty()) {
            if (hasLoginRequired) invalidateConfig()
            return null
        }

        val sortedVideo = allVideo.sortedByDescending { it.first }
        val qualities = sortedVideo.mapNotNull { (height, f) ->
            val url = f["url"]?.toString() ?: return@mapNotNull null
            if (height == 0 || url.isBlank()) return@mapNotNull null
            if (url.contains("?n=") || url.contains("&n=")) return@mapNotNull null
            YoutubeQuality(height = height, label = "${height}p", videoUrl = url)
        }

        if (qualities.isEmpty()) return null

        val bestAudio = allAudio
            .sortedByDescending { (it["bitrate"] as? Number)?.toDouble() ?: 0.0 }
            .firstOrNull()
            ?.let { it["url"]?.toString() }
            ?.takeIf { it.isNotBlank() }

        return YoutubeStreamResult(
            videoUrl = qualities.first().videoUrl,
            audioUrl = bestAudio,
            qualities = qualities,
        )
    }

    private fun parseQualityLabel(label: String?): Int? {
        if (label.isNullOrBlank()) return null
        return Regex("(\\d{2,4})p").find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private suspend fun resolvePiped(videoId: String): YoutubeStreamResult? {
        for (instance in pipedInstances) {
            val result = tryPipedStream(instance, videoId)
            if (result != null) return result
        }
        return null
    }

    private suspend fun tryPipedStream(instance: String, videoId: String): YoutubeStreamResult? {
        return try {
            withTimeout(10_000L) {
                val url = "$instance/streams/$videoId"
                val request = Request.Builder().url(url)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .build()
                val response = HttpClient.client.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body!!.string()
                val parsed = json.decodeFromString<PipedStreamsResponse>(body)

                val qualities = parsed.videoStreams
                    .filter { it.url.isNotBlank() }
                    .mapNotNull { item ->
                        val h = item.quality.filter { it.isDigit() }.toIntOrNull() ?: return@mapNotNull null
                        YoutubeQuality(height = h, label = item.quality, videoUrl = item.url)
                    }
                    .sortedByDescending { it.height }

                val hlsUrl = parsed.hls?.takeIf { it.isNotBlank() }
                if (hlsUrl != null) {
                    return@withTimeout YoutubeStreamResult(videoUrl = hlsUrl, qualities = qualities)
                }

                val best = qualities.firstOrNull() ?: return@withTimeout null
                val bestAudio = parsed.audioStreams
                    .filter { it.url.isNotBlank() }
                    .sortedByDescending { it.quality.filter { it.isDigit() }.toIntOrNull() ?: 0 }
                    .firstOrNull()?.url

                YoutubeStreamResult(videoUrl = best.videoUrl, audioUrl = bestAudio, qualities = qualities)
            }
        } catch (_: Exception) { null }
    }

    private suspend fun resolveNewPipe(videoId: String): YoutubeStreamResult? {
        return try {
            PlatformYouTubeSearch.initNewPipeIfNeeded()
            val service = ServiceList.YouTube
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamExtractor = service.getStreamExtractor(videoUrl)
            streamExtractor.fetchPage()

            val videoStreams = (streamExtractor.videoStreams as? List<*>)?.filterIsInstance<org.schabi.newpipe.extractor.stream.VideoStream>() ?: emptyList()
            val audioStreams = (streamExtractor.audioStreams as? List<*>)?.filterIsInstance<org.schabi.newpipe.extractor.stream.AudioStream>() ?: emptyList()

            val combined = videoStreams.filter { !it.isVideoOnly }.sortedByDescending { extractResolution(it.resolution) }
            val videoOnly = videoStreams.filter { it.isVideoOnly }.sortedByDescending { extractResolution(it.resolution) }

            val qualities = (combined + videoOnly).mapNotNull { vs ->
                val h = extractResolution(vs.resolution)
                if (h == 0 || vs.url.isNullOrEmpty()) null
                else YoutubeQuality(height = h, label = "${h}p", videoUrl = vs.url!!)
            }

            val bestVideo = qualities.firstOrNull()
            if (bestVideo != null) {
                val bestAudio = audioStreams.sortedByDescending { it.averageBitrate ?: 0 }.firstOrNull()?.url
                YoutubeStreamResult(videoUrl = bestVideo.videoUrl, audioUrl = bestAudio, qualities = qualities)
            } else {
                audioStreams.firstOrNull { !it.url.isNullOrEmpty() }
                    ?.let { YoutubeStreamResult(videoUrl = it.url ?: return@let null) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "NewPipe fallback failed: ${e.message}")
            null
        }
    }

    private fun extractResolution(resolution: String?): Int {
        if (resolution == null) return 0
        return resolution.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    @Serializable
    data class PipedStreamItem(val url: String = "", val quality: String = "", val format: String = "")
    @Serializable
    data class PipedStreamsResponse(
        val title: String = "", val uploader: String = "", val duration: Long = 0,
        val thumbnailUrl: String = "", val hls: String? = null,
        val videoStreams: List<PipedStreamItem> = emptyList(),
        val audioStreams: List<PipedStreamItem> = emptyList(),
    )
}
