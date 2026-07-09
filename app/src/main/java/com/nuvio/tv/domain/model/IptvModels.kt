package com.robbdeeze.nuviotv.domain.model

data class IptvChannel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val categoryName: String? = null
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
    val type: String // "m3u", "xtream", "stalker"
)
