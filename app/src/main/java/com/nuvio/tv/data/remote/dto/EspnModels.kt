package com.robbdeeze.nuviotv.data.remote.dto

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
    @Json(name = "shortName") val shortName: String? = null,
    @Json(name = "date") val date: String,
    @Json(name = "status") val status: EspnStatus,
    @Json(name = "competitions") val competitions: List<EspnCompetition>? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatus(
    @Json(name = "type") val type: EspnStatusType
)

@JsonClass(generateAdapter = true)
data class EspnStatusType(
    @Json(name = "name") val name: String,
    @Json(name = "detail") val detail: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "completed") val completed: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetition(
    @Json(name = "id") val id: String? = null,
    @Json(name = "competitors") val competitors: List<EspnCompetitor>? = null,
    @Json(name = "broadcasts") val broadcasts: List<EspnBroadcast>? = null,
    @Json(name = "status") val status: EspnStatus? = null,
    @Json(name = "notes") val notes: List<EspnNote>? = null,
    @Json(name = "logos") val logos: List<EspnLogo>? = null,
    @Json(name = "date") val date: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetitor(
    @Json(name = "id") val id: String,
    @Json(name = "homeAway") val homeAway: String,
    @Json(name = "team") val team: EspnTeam,
    @Json(name = "score") val score: String? = null,
    @Json(name = "winner") val winner: Boolean? = null,
    @Json(name = "records") val records: List<EspnRecord>? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeam(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "abbreviation") val abbreviation: String? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "alternateColor") val alternateColor: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "nickname") val nickname: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnBroadcast(
    @Json(name = "market") val market: String? = null,
    @Json(name = "names") val names: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class EspnRecord(
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnNote(
    @Json(name = "type") val type: String? = null,
    @Json(name = "headline") val headline: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnLogo(
    @Json(name = "href") val href: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingsResponse(
    @Json(name = "standings") val standings: List<EspnStandingContainer>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingContainer(
    @Json(name = "entries") val entries: List<EspnStandingEntry>? = null,
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingEntry(
    @Json(name = "team") val team: EspnTeam,
    @Json(name = "stats") val stats: List<EspnStandingStat>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingStat(
    @Json(name = "name") val name: String,
    @Json(name = "value") val value: String? = null,
    @Json(name = "displayValue") val displayValue: String? = null
)
