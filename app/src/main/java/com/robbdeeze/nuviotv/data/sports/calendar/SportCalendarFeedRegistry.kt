package com.robbdeeze.nuviotv.data.sports.calendar

/**
 * Maps SportNutz league ids (see [SportsLeague] in the hub ViewModel) to working
 * iCal feed URLs. Only feeds verified to return valid .ics are listed.
 *
 * Primary source: fixtur.es (league-level schedules via ics.fixtur.es/v2).
 * UFC uses the community-maintained UFC-cal feed (raw GitHub).
 */
object SportCalendarFeedRegistry {

    data class Feed(val sport: String, val league: String, val url: String)

    fun feedsFor(leagueId: String): List<Feed> = byLeague[leagueId] ?: emptyList()

    val byLeague: Map<String, List<Feed>> = mapOf(
        "epl" to listOf(feed("Soccer", "EPL", "https://ics.fixtur.es/v2/league/premier-league.ics")),
        "seriea" to listOf(feed("Soccer", "Serie A", "https://ics.fixtur.es/v2/league/serie-a.ics")),
        "bundesliga" to listOf(feed("Soccer", "Bundesliga", "https://ics.fixtur.es/v2/league/bundesliga.ics")),
        "ligue1" to listOf(feed("Soccer", "Ligue 1", "https://ics.fixtur.es/v2/league/ligue-1.ics")),
        "ucl" to listOf(feed("Soccer", "UCL", "https://ics.fixtur.es/v2/league/champions-league.ics")),
        "soccer" to listOf(feed("Soccer", "MLS", "https://ics.fixtur.es/v2/league/mls-major-league-soccer.ics")),
        "f1" to listOf(feed("Racing", "F1", "https://ics.fixtur.es/v2/league/formula-1.ics")),
        "nfl" to listOf(feed("Football", "NFL", "https://ics.fixtur.es/v2/league/nfl.ics")),
        "nba" to listOf(feed("Basketball", "NBA", "https://ics.fixtur.es/v2/league/nba.ics")),
        "wnba" to listOf(feed("Basketball", "WNBA", "https://ics.fixtur.es/v2/league/wnba.ics")),
        "ufc" to listOf(feed("Fighting", "UFC", "https://raw.githubusercontent.com/clarencechaan/ufc-cal/ics/UFC.ics")),
    )

    private fun feed(sport: String, league: String, url: String) = Feed(sport, league, url)
}