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

    private val mirrors = listOf(
        "https://streamsports99.ru",
    )

    suspend fun fetchEvents(): List<StreamSports99Event> = withContext(Dispatchers.IO) {
        for (mirror in mirrors) {
            try {
                val request = Request.Builder().url("$mirror/api")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: continue
                        return@withContext parseEvents(body)
                    }
                }
            } catch (_: Exception) {}
        }
        emptyList()
    }

    private fun parseEvents(body: String): List<StreamSports99Event> {
        val result = mutableListOf<StreamSports99Event>()
        try {
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(StreamSports99Event(
                    id = obj.getStr("id", ""),
                    title = obj.getStr("title", ""),
                    category = obj.getStr("category", ""),
                    streamUrl = obj.getStr("streamUrl", ""),
                    thumbnailUrl = obj.getStr("thumbnailUrl", ""),
                    startTime = obj.getStr("startTime", ""),
                ))
            }
        } catch (_: Exception) {}
        return result
    }

    private fun JSONObject.getStr(key: String, default: String): String =
        if (has(key)) getString(key) else default
}