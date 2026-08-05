package com.robbdeeze.nuviotv.domain.sports

import com.robbdeeze.nuviotv.data.remote.api.SportsClient
import com.robbdeeze.nuviotv.data.remote.api.Sync2CalClient
import com.robbdeeze.nuviotv.data.remote.api.TheSportsDbClient
import com.robbdeeze.nuviotv.data.remote.api.WikipediaClient
import com.robbdeeze.nuviotv.data.remote.dto.EspnCompetition
import com.robbdeeze.nuviotv.data.remote.dto.EspnCompetitor
import com.robbdeeze.nuviotv.data.remote.dto.EspnEvent
import com.robbdeeze.nuviotv.data.remote.dto.EspnStandingEntry
import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import com.robbdeeze.nuviotv.domain.model.SportEvent
import com.robbdeeze.nuviotv.domain.model.SportTeam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SportsRepository @Inject constructor(
    private val sportsClient: SportsClient,
    private val theSportsDbClient: TheSportsDbClient,
) {
    private val scoreboardCache = mutableMapOf<String, CachedScoreboard>()
    private val standingsCache = mutableMapOf<String, CachedStandings>()

    private data class CachedScoreboard(
        val events: List<EspnProcessedEvent>,
        val fetchedAt: Long,
    )
    private data class CachedStandings(
        val entries: List<TeamStanding>,
        val fetchedAt: Long,
    )

    companion object {
        private const val CACHE_TTL_MS = 60_000L
        private const val STANDINGS_CACHE_TTL_MS = 300_000L
        private const val MAX_RETRIES = 2

        fun extractDate(isoDate: String): String = try {
            val inst = Instant.parse(isoDate)
            val local = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault())
            "${local.monthValue}/${local.dayOfMonth}"
        } catch (_: Exception) { isoDate.take(10) }

        fun extractTime12h(isoDate: String): String? = try {
            val inst = Instant.parse(isoDate)
            val local = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault())
            val h = local.hour
            val m = local.minute
            val amPm = if (h < 12) "AM" else "PM"
            val h12 = if (h % 12 == 0) 12 else h % 12
            "$h12:${m.toString().padStart(2, '0')} $amPm"
        } catch (_: Exception) { null }
    }

    private val prioritizedSports = listOf(
        "mma/ufc" to "Fighting", "fighting/ufc" to "Fighting",
        "mma/boxing" to "Fighting", "fighting/boxing" to "Fighting",
        "mma/pfl" to "Fighting", "fighting/pfl" to "Fighting",
        "football/nfl" to "Football", "basketball/nba" to "Basketball",
        "baseball/mlb" to "Baseball", "hockey/nhl" to "Hockey",
        "football/college-football" to "Football",
        "basketball/mens-college-basketball" to "Basketball",
        "soccer/eng.1" to "Soccer", "soccer/usa.1" to "Soccer",
        "soccer/esp.1" to "Soccer", "soccer/ita.1" to "Soccer",
        "soccer/ger.1" to "Soccer", "soccer/fra.1" to "Soccer",
        "basketball/wnba" to "Basketball", "racing/f1" to "Racing",
        "golf/pga" to "Golf", "tennis/atp" to "Tennis",
    )

    private val leagueNames = mapOf(
        "ufc" to "UFC", "boxing" to "Boxing", "pfl" to "PFL",
        "nfl" to "NFL", "nba" to "NBA", "mlb" to "MLB", "nhl" to "NHL",
        "eng.1" to "EPL", "usa.1" to "MLS", "esp.1" to "LaLiga",
        "ita.1" to "SerieA", "ger.1" to "Bundesliga", "fra.1" to "Ligue1",
        "college-football" to "CFB", "mens-college-basketball" to "CBB",
        "wnba" to "WNBA", "f1" to "F1", "pga" to "Golf",
        "atp" to "Tennis", "wta" to "Tennis",
    )

    private fun cacheKey(sport: String, league: String) = "$sport/$league"

    fun isCacheValid(sport: String, league: String, ttl: Long = CACHE_TTL_MS): Boolean {
        val cached = scoreboardCache[cacheKey(sport, league)] ?: return false
        return (System.currentTimeMillis() - cached.fetchedAt) < ttl
    }

    fun getCached(sport: String, league: String): List<EspnProcessedEvent>? {
        return scoreboardCache[cacheKey(sport, league)]?.events
    }

    suspend fun fetchLeague(sport: String, league: String): List<EspnProcessedEvent> {
        val key = cacheKey(sport, league)
        if (isCacheValid(sport, league)) return scoreboardCache[key]!!.events

        val events = retryWithBackoff {
            val response = sportsClient.getScoreboard(sport, league)
            response.events?.mapNotNull { espnEvent ->
                processEvent(espnEvent, sport, league)
            } ?: emptyList()
        }

        scoreboardCache[key] = CachedScoreboard(events, System.currentTimeMillis())
        return events
    }

    suspend fun fetchAll(date: String? = null): List<EspnProcessedEvent> = coroutineScope {
        val allEvents = prioritizedSports.map { (path, sportType) ->
            async {
                val parts = path.split("/")
                val s = parts[0]
                val l = parts[1]
                try {
                    val events = fetchLeague(s, l)
                    events.map { it.copy(sport = sportType) }
                } catch (_: Exception) { emptyList() }
            }
        }.awaitAll().flatten()

        val live = allEvents.filter { it.isLive }
        val sorted = allEvents.sortedBy { it.date }
        (live + sorted).distinctBy { it.id }.take(80)
    }

    suspend fun fetchStandings(sport: String, league: String, season: Int? = null): List<TeamStanding> {
        val key = "standings_$sport/$league"
        val cached = standingsCache[key]
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAt) < STANDINGS_CACHE_TTL_MS) {
            return cached.entries
        }
        val entries = try {
            val response = sportsClient.getStandings(sport, league)
            val containers = response.standings ?: emptyList()
            val leagueAbbr = leagueNames[league] ?: league.uppercase()
            val allEntries = mutableListOf<TeamStanding>()
            for (container in containers) {
                val entries = container.entries ?: continue
                for ((i, entry) in entries.withIndex()) {
                    val team = entry.team
                    val stats = entry.stats ?: emptyList()
                    val wins = stats.find { it.name == "wins" }?.displayValue ?: "0"
                    val losses = stats.find { it.name == "losses" }?.displayValue ?: "0"
                    val ties = stats.find { it.name == "ties" }?.displayValue
                    val record = if (ties != null && ties != "0") "$wins-$losses-$ties" else "$wins-$losses"
                    val net = stats.find { it.name == "netRating" || it.name == "avgPointDifferential" }?.displayValue
                        ?: stats.find { it.name == "pointsFor" }?.displayValue ?: ""
                    allEntries.add(TeamStanding(
                        teamName = team.displayName,
                        logo = team.logo,
                        record = record,
                        league = leagueAbbr,
                        sport = sport,
                        rank = i + 1,
                        netRating = net,
                        location = team.location ?: "",
                    ))
                }
            }
            allEntries
        } catch (_: Exception) { emptyList() }
        standingsCache[key] = CachedStandings(entries, System.currentTimeMillis())
        return entries
    }

    private fun processEvent(espnEvent: EspnEvent, sportPath: String, league: String): EspnProcessedEvent? {
        val competitions = espnEvent.competitions ?: return null
        val sportType = detectSportType(sportPath)
        val leagueAbbr = leagueNames[league] ?: league.uppercase()

        if (sportType == "Fighting") {
            return processFightingEvent(espnEvent, competitions, leagueAbbr)
        }

        val competition = competitions.firstOrNull() ?: return null
        val competitors = competition.competitors ?: return null
        val home = competitors.find { it.homeAway == "home" } ?: return null
        val away = competitors.find { it.homeAway == "away" } ?: return null

        val homeTeam = home.team
        val awayTeam = away.team
        val status = competition.status ?: espnEvent.status
        val statusType = status.type
        val statusName = statusType.name
        val statusDetail = statusType.detail

        val broadcast = competition.broadcasts?.firstOrNull()
        val channel = broadcast?.names?.firstOrNull()

        val rawDate = espnEvent.date
        val dateStr = extractDate(rawDate)
        val timeStr = extractTime12h(rawDate)

        val isLive = statusName.uppercase() in setOf("STATUS_IN_PROGRESS", "IN_PROGRESS")
        val isFinished = statusType.completed == true || statusName.uppercase() in setOf("STATUS_FINAL", "FINAL", "STATUS_COMPLETE")
        val isUpcoming = !isLive && !isFinished

        return EspnProcessedEvent(
            id = espnEvent.id,
            title = espnEvent.shortName ?: espnEvent.name,
            shortName = espnEvent.shortName,
            homeTeam = homeTeam.displayName,
            awayTeam = awayTeam.displayName,
            homeScore = home.score,
            awayScore = away.score,
            homeLogo = homeTeam.logo,
            awayLogo = awayTeam.logo,
            homeRecord = home.records?.find { it.type == "total" }?.summary,
            awayRecord = away.records?.find { it.type == "total" }?.summary,
            channel = channel,
            status = statusName,
            detail = statusDetail,
            date = dateStr,
            rawDate = rawDate,
            timeStr = timeStr,
            sport = sportType,
            league = leagueAbbr,
            isLive = isLive,
            isFinished = isFinished,
            isUpcoming = isUpcoming,
            eventImage = espnEvent.thumbnail,
        )
    }

    private fun processFightingEvent(event: EspnEvent, competitions: List<EspnCompetition>, leagueAbbr: String): EspnProcessedEvent {
        val mainCompetition = competitions.firstOrNull()
        val competitors = mainCompetition?.competitors ?: emptyList()
        val home = competitors.find { it.homeAway == "home" }
        val away = competitors.find { it.homeAway == "away" }

        val rawDate = event.date
        val dateStr = extractDate(rawDate)
        val timeStr = extractTime12h(rawDate)

        val status = mainCompetition?.status ?: event.status
        val statusType = status.type
        val statusName = statusType.name
        val isLive = statusName.uppercase() in setOf("STATUS_IN_PROGRESS", "IN_PROGRESS")
        val isFinished = statusType.completed == true || statusName.uppercase() in setOf("STATUS_FINAL", "FINAL", "STATUS_COMPLETE")
        val isUpcoming = !isLive && !isFinished

        val totalFights = competitions.size
        val fightSuffix = if (totalFights > 1) " ($totalFights fights)" else ""
        val homeName = home?.team?.displayName ?: "TBD"
        val awayName = away?.team?.displayName ?: "TBD"

        val eventImage = mainCompetition?.logos?.firstOrNull()?.href
            ?: event.thumbnail
            ?: guessWikipediaImage(event.name)

        return EspnProcessedEvent(
            id = event.id,
            title = "$homeName vs $awayName$fightSuffix",
            shortName = event.shortName ?: event.name,
            homeTeam = homeName,
            awayTeam = awayName,
            homeScore = home?.score,
            awayScore = away?.score,
            homeLogo = home?.team?.logo,
            awayLogo = away?.team?.logo,
            eventImage = eventImage,
            status = statusName,
            detail = statusType.detail,
            date = dateStr,
            rawDate = rawDate,
            timeStr = timeStr,
            sport = "Fighting",
            league = leagueAbbr,
            isLive = isLive,
            isFinished = isFinished,
            isUpcoming = isUpcoming,
            subEventCount = totalFights,
        )
    }

    suspend fun searchSportVideos(eventName: String, team1: String, team2: String, isFuture: Boolean): List<HighlightVideo> = withContext(Dispatchers.IO) {
        val queries = mutableListOf<String>()
        if (isFuture) {
            queries.addAll(listOf(
                "$team1 vs $team2 preview",
                "$team1 vs $team2 predictions",
                "$eventName preview",
            ))
        } else {
            queries.addAll(listOf(
                "$team1 vs $team2 highlights",
                "$team1 vs $team2 full fight",
                "$eventName highlights",
                "$team1 $team2 match replay",
            ))
        }
        val seen = mutableSetOf<String>()
        val results = mutableListOf<HighlightVideo>()
        for (query in queries) {
            try {
                val videos = PlatformYouTubeSearch.search(query)
                for (v in videos) {
                    if (v.videoId in seen) continue
                    seen.add(v.videoId)
                    results.add(HighlightVideo(
                        eventId = "",
                        videoId = v.videoId,
                        title = v.title,
                        thumbnail = "https://img.youtube.com/vi/${v.videoId}/mqdefault.jpg",
                        channelName = v.channelName ?: "YouTube",
                        durationSeconds = 0,
                    ))
                    if (results.size >= 8) break
                }
            } catch (_: Exception) { }
            if (results.size >= 8) break
        }
        results
    }

    private fun guessWikipediaImage(eventName: String): String? {
        val cleanName = eventName
            .replace("UFC", "UFC_")
            .replace(" ", "_")
            .takeWhile { it != '(' }.trimEnd('_')
        return "https://upload.wikimedia.org/wikipedia/en/thumb/placeholder.jpg"
    }

    fun toSportEvent(processed: EspnProcessedEvent): SportEvent {
        return SportEvent(
            id = processed.id,
            name = processed.title,
            date = processed.rawDate ?: processed.date,
            status = processed.detail,
            homeTeam = SportTeam(id = "", name = processed.homeTeam, displayName = processed.homeTeam, logoUrl = processed.homeLogo),
            awayTeam = SportTeam(id = "", name = processed.awayTeam, displayName = processed.awayTeam, logoUrl = processed.awayLogo),
            homeScore = processed.homeScore,
            awayScore = processed.awayScore,
            leagueAbbreviation = processed.league,
        )
    }

    private suspend fun retryWithBackoff(block: suspend () -> List<EspnProcessedEvent>): List<EspnProcessedEvent> {
        var lastError: Exception? = null
        for (attempt in 0..MAX_RETRIES) {
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_RETRIES) {
                    kotlinx.coroutines.delay((attempt + 1) * 1000L)
                }
            }
        }
        throw lastError ?: Exception("Failed after retries")
    }

    private fun detectSportType(path: String): String = when {
        path.contains("ufc") || path.contains("boxing") || path.contains("pfl") || path.contains("bellator") -> "Fighting"
        path.contains("football") || path.contains("nfl") -> "Football"
        path.contains("basketball") || path.contains("nba") -> "Basketball"
        path.contains("baseball") || path.contains("mlb") -> "Baseball"
        path.contains("hockey") || path.contains("nhl") -> "Hockey"
        path.contains("soccer") -> "Soccer"
        path.contains("racing") || path.contains("f1") -> "Racing"
        path.contains("golf") -> "Golf"
        path.contains("tennis") -> "Tennis"
        else -> "Sports"
    }

    fun clearCache() {
        scoreboardCache.clear()
        standingsCache.clear()
    }

    fun getCachedStandings(sport: String, league: String): List<TeamStanding>? {
        return standingsCache["standings_$sport/$league"]?.entries
    }
}
