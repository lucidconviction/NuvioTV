package com.nuvio.tv.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EspnNewsResponse(
    @Json(name = "articles") val articles: List<EspnArticle>? = null
)

@JsonClass(generateAdapter = true)
data class EspnArticle(
    @Json(name = "headline") val headline: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "published") val published: String? = null,
    @Json(name = "images") val images: List<EspnImage>? = null
)

@JsonClass(generateAdapter = true)
data class EspnImage(
    @Json(name = "url") val url: String
)
