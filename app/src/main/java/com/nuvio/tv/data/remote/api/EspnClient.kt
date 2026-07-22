package com.robbdeeze.nuviotv.data.remote.api

import com.robbdeeze.nuviotv.data.remote.dto.EspnScoreboardResponse
import com.robbdeeze.nuviotv.data.remote.dto.EspnStandingsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface EspnClient {
    @GET("apis/site/v2/sports/{sport}/{league}/scoreboard")
    suspend fun getScoreboard(
        @Path("sport") sport: String,
        @Path("league") league: String
    ): EspnScoreboardResponse

    @GET("apis/site/v2/sports/{sport}/{league}/standings")
    suspend fun getStandings(
        @Path("sport") sport: String,
        @Path("league") league: String
    ): EspnStandingsResponse
}
