package com.nuvio.tv.data.iptv

import com.nuvio.tv.domain.model.IptvChannel
import com.nuvio.tv.domain.model.IptvCategory
import com.nuvio.tv.core.network.HttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

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
}
