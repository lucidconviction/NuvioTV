package com.nuvio.tv.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EspnScoreboardResponse(
    @Json(name = "events") val events: List<EspnEvent>? = null
)

@JsonClass(generateAdapter = true)
data class EspnEvent(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "date") val date: String,
    @Json(name = "status") val status: EspnStatus,
    @Json(name = "competitions") val competitions: List<EspnCompetition>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatus(
    @Json(name = "type") val type: EspnStatusType
)

@JsonClass(generateAdapter = true)
data class EspnStatusType(
    @Json(name = "name") val name: String,
    @Json(name = "detail") val detail: String
)

@JsonClass(generateAdapter = true)
data class EspnCompetition(
    @Json(name = "competitors") val competitors: List<EspnCompetitor>? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetitor(
    @Json(name = "id") val id: String,
    @Json(name = "homeAway") val homeAway: String,
    @Json(name = "team") val team: EspnTeam,
    @Json(name = "score") val score: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeam(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "alternateColor") val alternateColor: String? = null
)
