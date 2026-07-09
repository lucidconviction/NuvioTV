package com.nuvio.tv.domain.model

data class SportLeague(
    val id: String,
    val name: String,
    val abbreviation: String,
    val slug: String
)

data class SportTeam(
    val id: String,
    val name: String,
    val displayName: String,
    val logoUrl: String? = null,
    val color: String? = null,
    val alternateColor: String? = null
)

data class SportEvent(
    val id: String,
    val name: String,
    val date: String,
    val status: String,
    val homeTeam: SportTeam,
    val awayTeam: SportTeam,
    val homeScore: String? = null,
    val awayScore: String? = null,
    val leagueAbbreviation: String = "",
) {
    val isLive: Boolean get() = status.uppercase() in setOf(
        "IN PROGRESS", "LIVE", "PLAYING", "1ST", "2ND", "3RD", "4TH", "OT",
        "HALFTIME", "INTERMISSION", "DELAYED", "IN PROGRESS", "RAIN DELAY",
        "SOCCER 1ST", "SOCCER 2ND",
    )
}

data class SportEventVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val durationSeconds: Int,
    val category: String = "",
)

data class MatchedChannel(
    val channel: IptvChannel,
    val matchType: MatchType,
    val sourceName: String = "",
    val region: String = "",
)

enum class MatchType(val label: String) {
    LEAGUE("League"),
    TEAM("Team"),
    GENERAL_SPORTS("Sports"),
}

enum class EventTab(val label: String) {
    LIVE("Watch Live"),
    HIGHLIGHTS("Highlights"),
    PRE_MATCH("Pre-Match"),
}
