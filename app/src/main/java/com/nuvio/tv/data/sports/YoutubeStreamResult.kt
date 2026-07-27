package com.robbdeeze.nuviotv.data.sports

import com.robbdeeze.nuviotv.domain.model.YoutubeQuality

data class YoutubeStreamResult(
    val videoUrl: String,
    val audioUrl: String? = null,
    val qualities: List<YoutubeQuality> = emptyList(),
)
