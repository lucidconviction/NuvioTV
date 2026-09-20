package com.robbdeeze.nuviotv.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ExternalStreamsClient {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://streamed.pk/api"

    suspend fun getMatches(category: String, limit: Int = 25): List<ExternalStreamMatch> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/matches/$category?limit=$limit"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "NuvioTV/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val arr = JSONArray(body)
                    parseMatches(arr)
                }
            } catch (_: Exception) { emptyList() }
        }

    suspend fun getMatchSources(matchId: String): List<ExternalStreamSource> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/matches/$matchId/sources"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "NuvioTV/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val arr = JSONArray(body)
                    parseSources(arr)
                }
            } catch (_: Exception) { emptyList() }
        }

    private fun parseMatches(arr: JSONArray): List<ExternalStreamMatch> {
        val result = mutableListOf<ExternalStreamMatch>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { obj ->
                parseMatch(obj)?.let { result.add(it) }
            }
        }
        return result
    }

    private fun parseMatch(obj: JSONObject): ExternalStreamMatch? {
        return try {
            val teamsObj = obj.optJSONObject("teams")
            val home = teamsObj?.optJSONObject("home")
            val away = teamsObj?.optJSONObject("away")
            ExternalStreamMatch(
                id = obj.optGetString("id", "") ?: return null,
                title = obj.optGetString("title", "") ?: return null,
                category = obj.optGetString("category", ""),
                date = obj.optGetLong("date", 0L),
                popular = obj.optGetBoolean("popular", false),
                homeName = home?.optGetString("name", "") ?: "",
                awayName = away?.optGetString("name", "") ?: "",
                homeBadge = home?.optGetString("badge", "") ?: "",
                awayBadge = away?.optGetString("badge", "") ?: "",
                poster = obj.optGetString("poster", ""),
                sources = parseSources(obj.optJSONArray("sources") ?: JSONArray())
            )
        } catch (_: Exception) { null }
    }

    private fun parseSources(arr: JSONArray): List<ExternalStreamSource> {
        val result = mutableListOf<ExternalStreamSource>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { obj ->
                ExternalStreamSource(
                    source = obj.optGetString("source", ""),
                    id = obj.optGetString("id", "")
                ).let { result.add(it) }
            }
        }
        return result
    }

    private fun JSONObject.optGetString(key: String, default: String): String =
        if (this.has(key)) this.getString(key) else default

    private fun JSONObject.optGetLong(key: String, default: Long): Long =
        if (this.has(key)) this.getLong(key) else default

    private fun JSONObject.optGetBoolean(key: String, default: Boolean): Boolean =
        if (this.has(key)) this.getBoolean(key) else default

    /**
     * Resolve a stream source reference into a playable URL.
     * Sources are typically embedded player IDs (e.g. YouTube video IDs or
     * direct stream URLs). We return the id directly; callers can further
     * resolve YouTube IDs via YouTubeStreamResolver if needed.
     */
    fun resolveStreamUrl(source: ExternalStreamSource): String = source.id
}

data class ExternalStreamMatch(
    val id: String,
    val title: String,
    val category: String,
    val date: Long,
    val popular: Boolean = false,
    val homeName: String = "",
    val awayName: String = "",
    val homeBadge: String = "",
    val awayBadge: String = "",
    val poster: String = "",
    val sources: List<ExternalStreamSource> = emptyList()
) {
    val streamId: String get() = id
    val streamUrl: String get() = sources.firstOrNull()?.let { ExternalStreamsClient.resolveStreamUrl(it) } ?: ""
    val thumbUrl: String get() = poster
}

data class ExternalStreamSource(
    val source: String,
    val id: String
)