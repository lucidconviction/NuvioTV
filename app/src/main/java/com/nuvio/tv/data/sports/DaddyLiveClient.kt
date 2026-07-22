package com.robbdeeze.nuviotv.data.sports

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class DaddyLiveChannel(
    val name: String,
    val channelId: String,
    val embedUrl: String?,
)

data class DaddyLiveEvent(
    val id: String,
    val eventName: String,
    val category: String,
    val startTime: String,
    val day: String,
    val channels: List<DaddyLiveChannel>,
) {
    val startEpochMs: Long
        get() = parseEventTime(day, startTime)

    val isLive: Boolean
        get() {
            if (startEpochMs == 0L) return false
            val now = System.currentTimeMillis()
            return now >= startEpochMs - 3600000 && now < startEpochMs + 7200000
        }

    val localTime: String
        get() {
            if (startEpochMs == 0L) return startTime
            val local = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(startEpochMs), ZoneId.systemDefault())
            val h = local.hour
            val m = local.minute
            val amPm = if (h < 12) "AM" else "PM"
            val h12 = if (h % 12 == 0) 12 else h % 12
            return "$h12:${String.format("%02d", m)} $amPm"
        }

    val localDate: String
        get() {
            if (startEpochMs == 0L) return day
            val local = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(startEpochMs), ZoneId.systemDefault())
            return "${local.monthValue}/${local.dayOfMonth}"
        }

    companion object {
        private val dayFormat1 = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        private val dayFormat2 = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        private val dayFormat3 = DateTimeFormatter.ofPattern("MMMM dd, yyyy")

        fun parseEventTime(dayStr: String, timeStr: String): Long {
            return try {
                val cleanDay = dayStr.replace("Full Schedule ", "").replace("Schedule ", "").trim()
                val dayParsed = try {
                    java.time.LocalDate.parse(cleanDay, dayFormat1)
                } catch (_: Exception) {
                    try { java.time.LocalDate.parse(cleanDay, dayFormat2) }
                    catch (_: Exception) { java.time.LocalDate.parse(cleanDay, dayFormat3) }
                }
                val parts = timeStr.split(":")
                val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return 0
                val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return 0
                val isPM = timeStr.lowercase().contains("pm")
                val h24 = if (isPM && hour != 12) hour + 12 else if (!isPM && hour == 12) 0 else hour
                val ny = ZonedDateTime.of(dayParsed.year, dayParsed.monthValue, dayParsed.dayOfMonth, h24, minute, 0, 0, ZoneId.of("America/New_York"))
                ny.toInstant().toEpochMilli()
            } catch (_: Exception) { 0L }
        }
    }
}

object DaddyLiveClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mirrors = listOf(
        "https://daddylive.li",
        "https://daddylive.eu",
        "https://streameast.mov",
    )

    private val interestingCategories = setOf(
        "soccer", "football", "basketball", "baseball", "hockey", "tennis", "mma", "boxing",
        "rugby", "cricket", "motorsports", "f1", "motogp", "ufc", "nfl", "nba", "nhl", "mlb",
        "wwe", "darts", "snooker", "golf", "cycling", "volleyball", "handball",
        "olympics", "racing", "badminton", "table tennis", "water polo", "field hockey", "formula",
    )

    suspend fun fetchActiveDomain(): String? = withContext(Dispatchers.IO) {
        for (mirror in mirrors) {
            try {
                val request = Request.Builder().url("$mirror/api/events")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: continue
                        val arr = JSONArray(body)
                        if (arr.length() > 0) return@withContext mirror
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }

    suspend fun fetchEvents(): List<DaddyLiveEvent> = withContext(Dispatchers.IO) {
        val domain = fetchActiveDomain() ?: return@withContext emptyList()
        val result = mutableListOf<DaddyLiveEvent>()
        try {
            val request = Request.Builder().url("$domain/api/events")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val days = JSONArray(body)
                val seen = mutableSetOf<String>()
                var eventCounter = 0

                for (i in 0 until days.length()) {
                    val dayObj = days.getJSONObject(i)
                    val dayStr = dayObj.optString("day", "").trim()
                    val categories = dayObj.optJSONObject("categories") ?: continue
                    for (catKey in categories.keys()) {
                        val lowerCat = catKey.lowercase().trim()
                        val isInteresting = interestingCategories.any { lowerCat.contains(it) || it.contains(lowerCat) }
                        if (!isInteresting) continue
                        val events = categories.optJSONArray(catKey) ?: continue
                        for (j in 0 until events.length()) {
                            val ev = events.getJSONObject(j)
                            val eventName = ev.optString("event", "").trim()
                            val eventTime = ev.optString("time", "").trim()
                            if (eventName.isEmpty()) continue
                            val channelsArr = ev.optJSONArray("channels") ?: continue
                            val channels = mutableListOf<DaddyLiveChannel>()
                            for (k in 0 until channelsArr.length()) {
                                val ch = channelsArr.getJSONObject(k)
                                val chName = ch.optString("channel_name", "").trim()
                                val chId = ch.optString("channel_id", "").trim()
                                val chUrl = ch.optString("url", "").trim().ifEmpty { null }
                                if (chName.isNotEmpty()) {
                                    channels.add(DaddyLiveChannel(name = chName, channelId = chId, embedUrl = chUrl))
                                }
                            }
                            if (channels.isEmpty()) continue
                            val key = "$eventName|$eventTime|$dayStr"
                            if (seen.add(key)) {
                                result.add(DaddyLiveEvent(
                                    id = "dl_${eventCounter++}",
                                    eventName = eventName,
                                    category = catKey,
                                    startTime = eventTime,
                                    day = dayStr,
                                    channels = channels,
                                ))
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        result
    }
}
