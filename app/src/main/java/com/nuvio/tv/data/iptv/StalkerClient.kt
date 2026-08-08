package com.robbdeeze.nuviotv.data.iptv

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.core.network.HttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class StalkerClient(
    portalUrl: String,
    macAddress: String
) {
    private val baseUrl = portalUrl.trim().trimEnd('/')
    private val mac = macAddress.trim().uppercase()

    fun getChannels(): List<IptvChannel> {
        val token = getToken() ?: return emptyList()
        val channelJson = fetchJson("$baseUrl/stalker_portal/api/v1/channels?mac=$mac&token=$token&type=all")
        val data = extractDataArray(channelJson)
        val channels = mutableListOf<IptvChannel>()
        var counter = 0
        for (i in 0 until data.length()) {
            try {
                val obj = data.getJSONObject(i)
                val name = obj.optString("name").trim().ifBlank { obj.optString("title").trim() }.ifBlank { continue }
                val cmd = obj.optString("cmd").ifBlank { obj.optString("url") }
                val url = extractUrl(cmd) ?: continue
                counter++
                channels.add(
                    IptvChannel(
                        id = "stalker_${mac.hashCode()}_$counter",
                        name = name,
                        url = url,
                        logoUrl = obj.optString("logo").takeIf { it.isNotBlank() },
                        categoryName = obj.optString("genres").trim().ifBlank { "Other" }
                    )
                )
            } catch (_: Exception) { }
        }
        return channels
    }

    private fun getToken(): String? {
        val response = fetchJson("$baseUrl/stalker_portal/api/v1/portal/init?mac=$mac")
            ?: return null
        return try {
            val root = JSONObject(response)
            val token = root.optString("token").ifBlank { root.optString("index_token") }
            if (token.isNotBlank()) return token
            val data = root.optJSONObject("data")
            data?.let {
                it.optString("js_token").ifBlank { it.optString("api_token") }.ifBlank { it.optString("index_token") }
            }?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchJson(url: String): String? {
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").build()
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDataArray(jsonStr: String?): JSONArray {
        if (jsonStr.isNullOrBlank()) return JSONArray()
        return try {
            val root = JSONObject(jsonStr)
            root.optJSONArray("data") ?: root.optJSONObject("result")?.optJSONArray("data") ?: JSONArray()
        } catch (_: Exception) {
            try {
                JSONArray(jsonStr)
            } catch (_: Exception) {
                JSONArray()
            }
        }
    }

    private fun extractUrl(cmd: String?): String? {
        if (cmd.isNullOrBlank()) return null
        val parts = cmd.split(" ")
        val uri = parts.lastOrNull() ?: return null
        return when {
            uri.startsWith("http://") || uri.startsWith("https://") -> uri
            uri.startsWith("/") -> "$baseUrl$uri"
            uri.isNotBlank() -> "$baseUrl/$uri"
            else -> null
        }
    }
}
