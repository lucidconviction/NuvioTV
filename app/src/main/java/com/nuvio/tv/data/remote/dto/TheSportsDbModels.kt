package com.nuvio.tv.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TheSportsDbEvent(
    val idEvent: String = "",
    val strEvent: String = "",
    val strHomeTeam: String = "",
    val strAwayTeam: String = "",
    val intHomeScore: String? = null,
    val intAwayScore: String? = null,
    val dateEvent: String = "",
    val strStatus: String = "",
    val strSport: String = "",
    val strLeague: String = "",
    val strThumb: String? = null,
    val strVideo: String? = null,
    val strPoster: String? = null,
)

@Serializable
data class TheSportsDbEventList(
    val events: List<TheSportsDbEvent>? = null,
)
