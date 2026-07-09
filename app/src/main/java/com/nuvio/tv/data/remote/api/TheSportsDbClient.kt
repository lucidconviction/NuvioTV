package com.nuvio.tv.data.remote.api

import com.nuvio.tv.core.network.HttpClient
import com.nuvio.tv.data.remote.dto.TheSportsDbEventList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private const val THE_SPORTS_DB_BASE = "https://www.thesportsdb.com/api/v1/json"
private const val API_KEY = "123"

private val LEAGUE_IDS = mapOf(
    "nfl" to "4391",
    "nba" to "4387",
    "mlb" to "4424",
    "nhl" to "4380",
    "mls" to "4394",
    "ufc" to "4464",
    "pfl" to "",
    "boxing" to "",
    "powerslap" to "",
)

@Singleton
class TheSportsDbClient @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUpcomingEvents(leagueId: String): TheSportsDbEventList? = fetch("$THE_SPORTS_DB_BASE/$API_KEY/eventsnextleague.php?id=$leagueId")

    suspend fun getPastEvents(leagueId: String): TheSportsDbEventList? = fetch("$THE_SPORTS_DB_BASE/$API_KEY/eventspastleague.php?id=$leagueId")

    fun getLeagueId(slug: String): String {
        val sport = slug.split("/").firstOrNull()?.lowercase() ?: return ""
        return LEAGUE_IDS[sport] ?: ""
    }

    suspend fun searchByTeamName(query: String): TheSportsDbEventList? = fetch("$THE_SPORTS_DB_BASE/$API_KEY/searchevents.php?e=$query")

    private suspend fun fetch(url: String): TheSportsDbEventList? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body!!.string()
                json.decodeFromString<TheSportsDbEventList>(body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
