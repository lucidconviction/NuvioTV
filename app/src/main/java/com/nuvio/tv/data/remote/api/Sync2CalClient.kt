package com.robbdeeze.nuviotv.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Sync2CalClient {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://www.sync2cal.com/api/v2"

    suspend fun lookupBySlug(slug: String): Sync2CalCategory? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/categories/lookup?slug=${encodeParam(slug)}")
                .header("User-Agent", "NuvioTV/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = JSONObject(body)
                root.optJSONObject("category")?.let { parseCategory(it) }
            }
        } catch (_: Exception) { null }
    }

    suspend fun getFilteredEvents(uuid: String): List<Sync2CalEvent> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/categories/$uuid/filtered-events")
                .header("User-Agent", "NuvioTV/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                root.optJSONArray("events")?.let { parseEvents(it) } ?: emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseCategory(obj: JSONObject): Sync2CalCategory? {
        return try {
            Sync2CalCategory(
                id = obj.optInt("id", 0),
                name = obj.optString("name", "") ?: return null,
                uuid = obj.optString("uuid", "") ?: return null,
                slug = obj.optString("slug", ""),
                breadcrumb = obj.optString("breadcrumb", "").ifEmpty { null },
            )
        } catch (_: Exception) { null }
    }

    private fun parseEvents(arr: JSONArray): List<Sync2CalEvent> {
        val result = mutableListOf<Sync2CalEvent>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { obj ->
                parseEvent(obj)?.let { result.add(it) }
            }
        }
        return result
    }

    private fun parseEvent(obj: JSONObject): Sync2CalEvent? {
        return try {
            Sync2CalEvent(
                id = obj.optLong("id", 0),
                title = obj.optString("title", "") ?: return null,
                startTime = obj.optString("start_time", ""),
                endTime = obj.optString("end_time", ""),
                allDay = obj.optBoolean("all_day", false),
                location = obj.optString("location", "").ifEmpty { null },
                description = obj.optString("description", "").ifEmpty { null },
            )
        } catch (_: Exception) { null }
    }

    private fun encodeParam(s: String): String {
        return s.replace(" ", "+")
            .replace(",", "%2C")
            .replace("/", "%2F")
            .replace("&", "%26")
            .replace("?", "%3F")
            .replace("#", "%23")
    }
}

data class Sync2CalCategory(
    val id: Int,
    val name: String,
    val uuid: String,
    val slug: String,
    val breadcrumb: String? = null,
)

data class Sync2CalEvent(
    val id: Long,
    val title: String,
    val startTime: String,
    val endTime: String,
    val allDay: Boolean = false,
    val location: String? = null,
    val description: String? = null,
)

data class Sync2CalTvChannel(
    val name: String,
)
