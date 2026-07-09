package com.robbdeeze.nuviotv.domain

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.MatchedChannel
import com.robbdeeze.nuviotv.domain.model.MatchType
import com.robbdeeze.nuviotv.domain.model.SportEvent

object GameToChannelMatcher {

    private val leagueKeywords = mapOf(
        "NFL" to listOf("nfl", "football", "nfl network", "nfl redzone", "sunday night football", "monday night football", "thursday night football"),
        "NBA" to listOf("nba", "basketball", "nba tv", "tnt", "espn"),
        "MLB" to listOf("mlb", "baseball", "mlb network", "espn"),
        "NHL" to listOf("nhl", "hockey", "nhl network", "tnt"),
        "MLS" to listOf("mls", "soccer", "fox soccer"),
    )

    private val generalSportsKeywords = listOf("sports", "espn", "fox sports", "cbs sports", "nbc sports")

    fun matchChannels(event: SportEvent, channels: List<IptvChannel>, sourceName: String = ""): List<MatchedChannel> {
        val matches = mutableListOf<MatchedChannel>()

        for (channel in channels) {
            val channelName = channel.name.lowercase()
            val categoryName = (channel.categoryName ?: "").lowercase()

            val match = when {
                matchesByLeagueAbbreviation(event.leagueAbbreviation, channelName, categoryName) ->
                    MatchedChannel(channel, MatchType.LEAGUE, sourceName)
                matchesByTeamName(event.homeTeam.displayName, event.awayTeam.displayName, channelName, categoryName) ->
                    MatchedChannel(channel, MatchType.TEAM, sourceName)
                matchesGeneralSports(channelName, categoryName) ->
                    MatchedChannel(channel, MatchType.GENERAL_SPORTS, sourceName)
                else -> null
            }
            if (match != null) {
                matches.add(match)
            }
        }

        // Sort: league matches first, then team, then general sports
        return matches.sortedBy { it.matchType.ordinal }
    }

    private fun matchesByLeagueAbbreviation(league: String, channelName: String, category: String): Boolean {
        if (league.isBlank()) return false
        val key = league.uppercase()
        val keywords = leagueKeywords[key] ?: listOf(league.lowercase())
        return keywords.any { kw -> channelName.contains(kw) || category.contains(kw) }
    }

    private fun matchesByTeamName(homeTeam: String, awayTeam: String, channelName: String, category: String): Boolean {
        val teamTokens = (homeTeam.split(" ") + awayTeam.split(" "))
            .map { it.lowercase().trim() }
            .filter { it.length > 2 && it !in commonSkipWords }
        return teamTokens.any { token ->
            channelName.contains(token) || category.contains(token)
        }
    }

    private fun matchesGeneralSports(channelName: String, category: String): Boolean {
        return generalSportsKeywords.any { kw ->
            channelName.contains(kw) || category.contains(kw)
        }
    }

    private val commonSkipWords = setOf("the", "and", "for", "fc", "utd", "vs", "at", "de", "los", "las", "san", "real", "city", "united", "team", "club")
}
