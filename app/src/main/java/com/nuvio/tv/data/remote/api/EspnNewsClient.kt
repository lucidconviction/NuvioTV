package com.robbdeeze.nuviotv.data.remote.api

import com.robbdeeze.nuviotv.data.remote.dto.EspnNewsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface EspnNewsClient {
    @GET("apis/site/v2/sports/{sport}/{league}/news")
    suspend fun getNews(
        @Path("sport") sport: String,
        @Path("league") league: String
    ): EspnNewsResponse
}
