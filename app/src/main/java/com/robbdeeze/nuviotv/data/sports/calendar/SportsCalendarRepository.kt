package com.robbdeeze.nuviotv.data.sports.calendar

import com.robbdeeze.nuviotv.domain.model.SportEvent
import com.robbdeeze.nuviotv.domain.model.SportTeam
import java.time.Instant

/**
 * Aggregates iCal calendar feeds per SportNutz league and maps them to [SportEvent]s.
 * Schedules are a great fallback when ESPN/TheSportsDB return nothing for a league
 * (off-season, minor leagues, etc.).
 */
object SportsCalendarRepository {

    private val feedClient = CalendarFeedClient()
    private val leagueCache = mutableMapOf<String, CacheEntry>()

    private data class CacheEntry(val events: List<SportEvent>, val fetchedAt: Long)

    private const val CACHE_TTL_MS = 30 * 60 * 1000L

    fun isCachedFresh(leagueId: String): Boolean {
        val entry = leagueCache[leagueId] ?: return false
        return System.currentTimeMillis() - entry.fetchedAt < CACHE_TTL_MS
    }

    fun getCached(leagueId: String): List<SportEvent>? = leagueCache[leagueId]?.events

    /**
     * Returns upcoming calendar events for a league as [SportEvent]s. Never throws;
     * returns empty list on any failure so callers keep their existing fallback chain.
     */
    suspend fun upcomingFor(leagueId: String, leagueAbbr: String, hoursWindow: Long = 7 * 24L): List<SportEvent> {
        leagueCache[leagueId]?.let { entry ->
            if (System.currentTimeMillis() - entry.fetchedAt < CACHE_TTL_MS) return entry.events
        }

        val feeds = SportCalendarFeedRegistry.feedsFor(leagueId)
        if (feeds.isEmpty()) return emptyList()

        val now = Instant.now()
        val horizon = now.plusSeconds(hoursWindow * 3600L)
        val events = mutableListOf<SportEvent>()
        for (feed in feeds) {
            try {
                val parsed = feedClient.fetchIcs(feed.url, source = "fixtur.es", league = feed.league)
                parsed.events
                    .filter { !it.allDay }
                    .filter { it.startEpochMillis in now.toEpochMilli()..horizon.toEpochMilli() }
                    .sortedBy { it.startEpochMillis }
                    .mapNotNull { it.toSportEvent(feed.sport, leagueAbbr) }
                    .forEach { events.add(it) }
            } catch (_: Exception) {
                // feed unreachable/parse failure — keep the rest
            }
        }

        val result = events.distinctBy { it.id }.take(60)
        leagueCache[leagueId] = CacheEntry(result, System.currentTimeMillis())
        return result
    }

    /** Aggregates upcoming events across all leagues with registered feeds. */
    suspend fun allUpcoming(now: Instant = Instant.now(), hoursWindow: Long = 7 * 24L): List<SportEvent> {
        val result = mutableListOf<SportEvent>()
        for ((leagueId, feeds) in SportCalendarFeedRegistry.byLeague) {
            val abbr = leagueAbbrFor(leagueId)
            for (feed in feeds) {
                try {
                    val parsed = feedClient.fetchIcs(feed.url, source = "fixtur.es", league = feed.league)
                    val horizon = now.plusSeconds(hoursWindow * 3600L)
                    parsed.events
                        .filter { !it.allDay }
                        .filter { it.startEpochMillis in now.toEpochMilli()..horizon.toEpochMilli() }
                        .sortedBy { it.startEpochMillis }
                        .mapNotNull { it.toSportEvent(feed.sport, abbr) }
                        .forEach { result.add(it) }
                } catch (_: Exception) {
                    // skip failing feeds, keep the rest
                }
            }
        }
        return result.distinctBy { it.id }.take(120)
    }

    fun clearCache() {
        leagueCache.clear()
        feedClient.clearCache()
    }

    private fun leagueAbbrFor(leagueId: String): String = when (leagueId) {
        "epl" -> "EPL"; "seriea" -> "SerieA"; "bundesliga" -> "Bundesliga"; "ligue1" -> "Ligue1"
        "ucl" -> "UCL"; "soccer" -> "MLS"; "f1" -> "F1"; "nfl" -> "NFL"
        "nba" -> "NBA"; "wnba" -> "WNBA"; "ufc" -> "UFC"
        else -> leagueId.uppercase()
    }

    private fun CalendarEvent.toSportEvent(sport: String, leagueAbbr: String): SportEvent? {
        val (home, away) = parseTeams(title)
        val displayHome = home?.takeIf { it.isNotBlank() } ?: return null
        val displayAway = away?.takeIf { it.isNotBlank() } ?: leagueAbbr
        return SportEvent(
            id = "cal_${sport.lowercase()}_${uid}",
            name = title,
            date = Instant.ofEpochMilli(startEpochMillis).toString(),
            status = "Scheduled",
            homeTeam = SportTeam(id = "", name = displayHome, displayName = displayHome),
            awayTeam = SportTeam(id = "", name = displayAway, displayName = displayAway),
            leagueAbbreviation = leagueAbbr,
        )
    }

    private fun parseTeams(title: String): Pair<String?, String?> {
        val clean = title.replace(Regex("\\([^)]*\\)"), "").trim()
        val vsIdx = clean.indexOf(" vs ")
        if (vsIdx >= 0) {
            return clean.substring(0, vsIdx).trim() to clean.substring(vsIdx + 4).trim()
        }
        val dashIdx = clean.lastIndexOf(" - ")
        if (dashIdx >= 0) {
            return clean.substring(0, dashIdx).trim() to clean.substring(dashIdx + 3).trim()
        }
        return null to null
    }
}