package com.robbdeeze.nuviotv.domain.model

import java.time.LocalDate

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
    val isLive: Boolean get() {
        val s = status.uppercase()
        return s in setOf(
            "IN PROGRESS", "LIVE", "PLAYING", "1ST", "2ND", "3RD", "4TH", "OT",
            "HALFTIME", "INTERMISSION", "DELAYED", "IN PROGRESS", "RAIN DELAY",
            "SOCCER 1ST", "SOCCER 2ND",
        ) || s.contains("QUARTER") || s.contains("PERIOD") || s.contains("INNING") ||
            s.contains("HALF") || s.contains(" TOP") || s.contains(" BOT ") ||
            s.startsWith("Q") || s.startsWith("P") || s.startsWith("TOP") || s.startsWith("BOTTOM")
    }
    val isFinished: Boolean get() = status.uppercase() in setOf(
        "FINAL", "FULL TIME", "FT", "ENDED", "COMPLETE", "FINAL OT",
        "FINAL/OT", "FINAL 2OT", "FINAL SO", "FINAL/SO", "CANCELED",
        "POSTPONED", "SUSPENDED", "FORFEIT", "CANCELLED", "ABANDONED"
    )
    val isUpcoming: Boolean get() = !isLive && !isFinished
}

data class SportEventVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val durationSeconds: Int,
    val category: String = "",
)

fun SportEventVideo.toHighlightVideo(): com.robbdeeze.nuviotv.domain.sports.HighlightVideo =
    com.robbdeeze.nuviotv.domain.sports.HighlightVideo(
        eventId = videoId, videoId = videoId, title = title,
        thumbnail = thumbnailUrl, channelName = channelName, durationSeconds = durationSeconds,
        sport = category,
    )

data class SportCalendarDay(
    val date: LocalDate,
    val events: List<SportEvent>,
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
