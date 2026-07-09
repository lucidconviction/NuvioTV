package com.nuvio.tv.data.remote.api

import com.nuvio.tv.data.remote.dto.EspnScoreboardResponse
import com.nuvio.tv.data.remote.dto.EspnNewsResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SportsClient @Inject constructor(
    private val espnClient: EspnClient,
    private val espnNewsClient: EspnNewsClient
) {
    suspend fun getScoreboard(sport: String, league: String): EspnScoreboardResponse {
        return espnClient.getScoreboard(sport, league)
    }

    suspend fun getNews(sport: String, league: String): EspnNewsResponse {
        return espnNewsClient.getNews(sport, league)
    }
}
