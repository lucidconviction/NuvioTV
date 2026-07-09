package com.robbdeeze.nuviotv.data.remote.api

import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeHighlightClient @Inject constructor() {
    suspend fun getHighlights(homeTeam: String, awayTeam: String): String? {
        val query = "$homeTeam vs $awayTeam highlights"
        val results = PlatformYouTubeSearch.search(query)
        return results.firstOrNull()?.videoId
    }
}
