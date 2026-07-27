package com.robbdeeze.nuviotv.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WikipediaClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private const val WIKI_API = "https://en.wikipedia.org/api/rest_v1/page/summary"
    private val thumbnailCache = mutableMapOf<String, String?>()
    private val fetchedPages = mutableSetOf<String>()

    suspend fun getThumbnail(eventName: String): String? = withContext(Dispatchers.IO) {
        val pageName = guessPageName(eventName) ?: return@withContext null
        if (pageName in fetchedPages) return@withContext thumbnailCache[pageName]
        fetchedPages.add(pageName)

        try {
            val encoded = pageName.replace(" ", "_")
            val request = Request.Builder()
                .url("$WIKI_API/$encoded")
                .header("User-Agent", "NuvioTV/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val thumbnail = json.optJSONObject("thumbnail")?.optString("source")
                thumbnailCache[pageName] = thumbnail
                thumbnail
            }
        } catch (_: Exception) { null }
    }

    private fun guessPageName(eventName: String): String? {
        val clean = eventName
            .replace(" @ ", " vs ")
            .replace(" at ", " vs ")
            .trim()
        val parts = clean.split(" vs ", ignoreCase = true)
        if (parts.size >= 2) return "${parts[0].trim()} vs ${parts[1].trim()}"
        return clean.takeIf { it.isNotBlank() }
    }
}
