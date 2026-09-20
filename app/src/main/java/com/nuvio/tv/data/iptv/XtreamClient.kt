package com.robbdeeze.nuviotv.data.iptv

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.IptvCategory
import com.robbdeeze.nuviotv.domain.model.IptvEpgEntry
import com.robbdeeze.nuviotv.domain.model.IptvSeries
import com.robbdeeze.nuviotv.domain.model.IptvSeriesEpisode
import com.robbdeeze.nuviotv.domain.model.IptvSeriesSeason
import com.robbdeeze.nuviotv.domain.model.IptvVodItem
import com.robbdeeze.nuviotv.core.network.HttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class XtreamClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    fun getCategories(): List<IptvCategory> {
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_live_categories"
        val request = Request.Builder().url(url).build()
        try {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val json = JSONArray(response.body?.string() ?: "[]")
                val categories = mutableListOf<IptvCategory>()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    categories.add(
                        IptvCategory(
                            id = obj.getString("category_id"),
                            name = obj.getString("category_name")
                        )
                    )
                }
                return categories
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getChannels(): List<IptvChannel> {
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_live_streams"
        val request = Request.Builder().url(url).build()
        try {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val json = JSONArray(response.body?.string() ?: "[]")
                val channels = mutableListOf<IptvChannel>()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val streamId = obj.getInt("stream_id")
                    val containerExtension = obj.optString("container_extension", "ts")
                    val streamUrl = "$baseUrl/live/$username/$password/$streamId.$containerExtension"
                    channels.add(
                        IptvChannel(
                            id = streamId.toString(),
                            name = obj.getString("name"),
                            url = streamUrl,
                            logoUrl = obj.optString("stream_icon").takeIf { it.isNotBlank() },
                            categoryName = obj.optString("category_id")
                        )
                    )
                }
                return channels
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getEpgForChannel(streamId: String): List<IptvEpgEntry> {
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_simple_data_table&stream_id=$streamId&stream_type=live"
        val request = Request.Builder().url(url).build()
        try {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val json = JSONArray(response.body?.string() ?: "[]")
                val entries = mutableListOf<IptvEpgEntry>()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val title = obj.getString("title").ifBlank { continue }
                    val startStr = obj.getString("start")
                    val stopStr = obj.getString("stop")
                    val startMs = parseTimeToMillis(startStr)
                    val stopMs = parseTimeToMillis(stopStr)
                    entries.add(
                        IptvEpgEntry(
                            title = title,
                            description = obj.getString("description").takeIf { it.isNotBlank() },
                            startTimeMs = startMs,
                            endTimeMs = stopMs
                        )
                    )
                }
                return entries
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getEpgXmltv(): Map<String, List<IptvEpgEntry>> {
        val url = "$baseUrl/xmltv.php?username=$username&password=$password"
        val request = Request.Builder().url(url).build()
        try {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyMap()
                return parseXmltv(response.body!!.byteStream())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyMap()
        }
    }

    private fun parseXmltv(stream: java.io.InputStream): Map<String, List<IptvEpgEntry>> {
        val epgMap = mutableMapOf<String, MutableList<IptvEpgEntry>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(stream, "UTF-8")

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return epgMap
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

    fun getVod(): List<IptvVodItem> {
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_vod_streams"
        val request = Request.Builder().url(url).build()
        try {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val json = JSONArray(response.body?.string() ?: "[]")
                val vodItems = mutableListOf<IptvVodItem>()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val streamId = obj.getInt("stream_id")
                    val containerExtension = obj.optString("container_extension", "mp4")
                    val streamUrl = "$baseUrl/movie/$username/$password/$streamId.$containerExtension"
                    vodItems.add(
                        IptvVodItem(
                            id = streamId.toString(),
                            name = obj.getString("name"),
                            streamUrl = streamUrl,
                            streamType = containerExtension,
                            logoUrl = obj.optString("stream_icon").takeIf { it.isNotBlank() },
                            plot = obj.optString("plot").takeIf { it.isNotBlank() },
                            releaseYear = obj.optString("releasedate").takeIf { it.isNotBlank() },
                            duration = obj.optString("duration").takeIf { it.isNotBlank() },
                            rating = obj.optString("rating").takeIf { it.isNotBlank() },
                            category = obj.optString("category_id").takeIf { it.isNotBlank() },
                            backdrop = obj.optString("backdrop_path").takeIf { it.isNotBlank() },
                            added = obj.optString("added").takeIf { it.isNotBlank() }
                        )
                    )
                }
                return vodItems
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getSeries(): List<IptvSeries> {
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_series"
        val request = Request.Builder().url(url).build()
        try {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val json = JSONArray(response.body?.string() ?: "[]")
                val seriesList = mutableListOf<IptvSeries>()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val seriesId = obj.getInt("series_id")
                    val series = IptvSeries(
                        id = seriesId.toString(),
                        name = obj.getString("name"),
                        logoUrl = obj.optString("cover").takeIf { it.isNotBlank() },
                        plot = obj.optString("plot").takeIf { it.isNotBlank() },
                        releaseYear = obj.optString("releaseDate").takeIf { it.isNotBlank() },
                        rating = obj.optString("rating").takeIf { it.isNotBlank() },
                        backdrop = obj.optString("backdrop_path").takeIf { it.isNotBlank() },
                        category = obj.optString("category_id").takeIf { it.isNotBlank() }
                    )
                    seriesList.add(series)
                }
                return seriesList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getSeriesInfo(seriesId: String): IptvSeries? {
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_series_info&series_id=$seriesId"
        val request = Request.Builder().url(url).build()
        try {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val obj = JSONObject(response.body?.string() ?: "{}")
                val info = obj.getJSONObject("info")
                val episodesJson = obj.getJSONArray("episodes")
                val seasons = mutableListOf<IptvSeriesSeason>()
                val episodesBySeason = mutableMapOf<String, MutableList<IptvSeriesEpisode>>()
                for (i in 0 until episodesJson.length()) {
                    val ep = episodesJson.getJSONObject(i)
                    val seasonNum = ep.optString("season", "1")
                    val epNum = ep.optString("episode_num", "1")
                    val epId = ep.getInt("id").toString()
                    val containerExtension = ep.optString("container_extension", "mp4")
                    val streamUrl = "$baseUrl/series/$username/$password/$epId.$containerExtension"
                    val episode = IptvSeriesEpisode(
                        id = epId,
                        name = ep.getString("title"),
                        streamUrl = streamUrl,
                        logoUrl = ep.optString("container_extension").takeIf { it.isNotBlank() }?.let { ep.optString("stream_icon") },
                        duration = ep.optString("duration").takeIf { it.isNotBlank() },
                        episodeNumber = epNum,
                        seasonNumber = seasonNum
                    )
                    episodesBySeason.getOrPut(seasonNum) { mutableListOf() }.add(episode)
                }
                episodesBySeason.entries.forEach { (seasonNum, eps) ->
                    seasons.add(IptvSeriesSeason(
                        id = seasonNum,
                        name = "Season $seasonNum",
                        episodes = eps
                    ))
                }
                return IptvSeries(
                    id = seriesId,
                    name = info.getString("name"),
                    logoUrl = info.optString("cover").takeIf { it.isNotBlank() },
                    plot = info.optString("plot").takeIf { it.isNotBlank() },
                    releaseYear = info.optString("releaseDate").takeIf { it.isNotBlank() },
                    rating = info.optString("rating").takeIf { it.isNotBlank() },
                    backdrop = info.optString("backdrop_path").takeIf { it.isNotBlank() },
                    seasons = seasons,
                    category = info.optString("category_id").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}