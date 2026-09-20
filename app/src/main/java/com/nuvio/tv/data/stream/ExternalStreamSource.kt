package com.nuvio.tv.data.stream

data class ExternalStreamSource(
    val name: String,
    val url: String,
    val sport: String,
    val logo: String? = null,
    val quality: String = "HD"
)