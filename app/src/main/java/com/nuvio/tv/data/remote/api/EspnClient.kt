package com.nuvio.tv.data.remote.api

import com.nuvio.tv.data.remote.dto.EspnScoreboardResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface EspnClient {
    @GET("apis/site/v2/sports/{sport}/{league}/scoreboard")
    suspend fun getScoreboard(
        @Path("sport") sport: String,
        @Path("league") league: String
    ): EspnScoreboardResponse
}
