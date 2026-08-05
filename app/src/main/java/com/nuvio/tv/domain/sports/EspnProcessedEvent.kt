package com.robbdeeze.nuviotv.domain.sports

data class EspnProcessedEvent(
    val id: String,
    val title: String,
    val shortName: String? = null,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String? = null,
    val awayScore: String? = null,
    val homeLogo: String? = null,
    val awayLogo: String? = null,
    val homeRecord: String? = null,
    val awayRecord: String? = null,
    val eventImage: String? = null,
    val channel: String? = null,
    val status: String,
    val detail: String,
    val date: String,
    val rawDate: String? = null,
    val timeStr: String? = null,
    val sport: String = "",
    val league: String = "",
    val isLive: Boolean = false,
    val isFinished: Boolean = false,
    val isUpcoming: Boolean = false,
    val subEventCount: Int? = null,
)

data class TeamStanding(
    val teamName: String,
    val logo: String? = null,
    val record: String = "",
    val league: String = "",
    val sport: String = "",
    val rank: Int = 0,
    val netRating: String = "",
    val location: String = "",
)

data class HighlightVideo(
    val eventId: String,
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val durationSeconds: Int,
    val sport: String = "",
)
