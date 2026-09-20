package com.robbdeeze.nuviotv.data.sports

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class StreamSports99Event(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val thumbnailUrl: String = "",
    val startTime: String = "",
)

object StreamSports99Client {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val endpoints = listOf(
        "https://streamsports99.ru/api/matches",
        "https://streamsports99.ru/api/events",
        "https://streamsports99.ru/api",
    )

    suspend fun fetchEvents(): List<StreamSports99Event> = withContext(Dispatchers.IO) {
        for (url in endpoints) {
            try {
                val request = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val parsed = parseEvents(body)
                        if (parsed.isNotEmpty()) return@withContext parsed
                    }
                }
            } catch (_: Exception) {}
        }
        emptyList()
    }

    private fun parseEvents(body: String): List<StreamSports99Event> {
        val result = mutableListOf<StreamSports99Event>()
        try {
            val trimmed = body.trim()
            val arr = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("matches") ?: obj.optJSONArray("events") ?: obj.optJSONArray("data") ?: JSONArray()
            } else {
                JSONArray()
            }
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getStr("id", "")
                val title = obj.getStr("title", "")
                val category = obj.getStr("category", "Sports")
                val explicitUrl = obj.getStr("streamUrl", "").ifBlank { obj.getStr("url", "") }
                val streamUrl = if (explicitUrl.isNotBlank()) explicitUrl else if (id.isNotBlank()) "https://streamsports99.ru/watch/$id" else "https://streamsports99.ru"
                if (title.isNotBlank()) {
                    result.add(StreamSports99Event(
                        id = id.ifBlank { "ss99_$i" },
                        title = title,
                        category = category,
                        streamUrl = streamUrl,
                        thumbnailUrl = obj.getStr("thumbnailUrl", "").ifBlank { obj.getStr("poster", "") },
                        startTime = obj.getStr("startTime", "Live"),
                    ))
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun JSONObject.getStr(key: String, default: String): String =
        if (has(key) && !isNull(key)) getString(key) else default
}