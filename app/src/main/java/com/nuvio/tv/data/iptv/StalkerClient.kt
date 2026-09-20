package com.robbdeeze.nuviotv.data.iptv

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.IptvEpgEntry
import com.robbdeeze.nuviotv.core.network.HttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

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

    fun getEpg(): Map<String, List<IptvEpgEntry>> {
        val token = getToken() ?: return emptyMap()
        val epgJson = fetchJson("$baseUrl/stalker_portal/api/v1/epg?mac=$mac&token=$token&type=all")
            ?: return emptyMap()
        return try {
            val root = JSONObject(epgJson)
            val data = root.optJSONArray("data") ?: root.optJSONObject("result")?.optJSONArray("data")
                ?: return emptyMap()
            val epgMap = mutableMapOf<String, MutableList<IptvEpgEntry>>()
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val channelId = obj.getString("channel_id").ifBlank { continue }
                val title = obj.getString("title").ifBlank { continue }
                val startStr = obj.getString("start")
                val stopStr = obj.getString("stop")
                val startMs = parseTimeToMillis(startStr)
                val stopMs = parseTimeToMillis(stopStr)
                epgMap.getOrPut(channelId) { mutableListOf() }.add(
                    IptvEpgEntry(
                        title = title,
                        description = obj.getString("description").takeIf { it.isNotBlank() },
                        startTimeMs = startMs,
                        endTimeMs = stopMs
                    )
                )
            }
            epgMap
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    fun getEpgXmltv(): Map<String, List<IptvEpgEntry>> {
        val token = getToken() ?: return emptyMap()
        val resp = fetchJson("$baseUrl/stalker_portal/api/v1/epg_xmltv?mac=$mac&token=$token")
            ?: return emptyMap()
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(java.io.ByteArrayInputStream(resp.toByteArray(Charsets.UTF_8)), "UTF-8")

            val epgMap = mutableMapOf<String, MutableList<IptvEpgEntry>>()
            var channelId: String? = null
            var currentTitle: String? = null
            var currentStart: Long = 0
            var currentStop: Long = 0
            var currentDesc: String? = null

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
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
                    XmlPullParser.END_TAG -> {
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
            epgMap
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
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

    private fun parseTimeToMillis(timeStr: String?): Long {
        if (timeStr.isNullOrBlank()) return 0
        return try {
            timeStr.toLong() * 1000
        } catch (e: Exception) {
            try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                format.parse(timeStr)?.time ?: 0
            } catch (e2: Exception) {
                0
            }
        }
    }
}
