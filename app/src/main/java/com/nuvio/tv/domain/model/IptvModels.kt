package com.robbdeeze.nuviotv.domain.model

data class IptvChannel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val categoryName: String? = null,
    val audioUrl: String? = null,
    val epgUrl: String? = null,
    val qualities: List<YoutubeQuality> = emptyList(),
)

data class YoutubeQuality(
    val height: Int,
    val label: String,
    val videoUrl: String,
)

data class IptvCategory(
    val id: String,
    val name: String
)

data class IptvEpgEntry(
    val title: String,
    val description: String? = null,
    val startTimeMs: Long,
    val endTimeMs: Long
)

data class IptvSource(
    val name: String,
    val url: String,
    val type: String, // "m3u", "xtream", "stalker"
    val epgUrl: String? = null
)

data class IptvVodItem(
    val id: String,
    val name: String,
    val streamUrl: String,
    val streamType: String = "mp4",
    val logoUrl: String? = null,
    val plot: String? = null,
    val releaseYear: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val category: String? = null,
    val backdrop: String? = null,
    val added: String? = null,
)

data class IptvSeries(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val plot: String? = null,
    val releaseYear: String? = null,
    val rating: String? = null,
    val backdrop: String? = null,
    val seasons: List<IptvSeriesSeason> = emptyList(),
    val category: String? = null,
)

data class IptvSeriesSeason(
    val id: String,
    val name: String,
    val episodes: List<IptvSeriesEpisode> = emptyList(),
)

data class IptvSeriesEpisode(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val duration: String? = null,
    val episodeNumber: String? = null,
    val seasonNumber: String? = null,
)
