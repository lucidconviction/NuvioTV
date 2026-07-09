package com.nuvio.tv.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface TvMazeClient {
    @GET("search/shows")
    suspend fun searchShows(
        @Query("q") query: String
    ): List<Map<String, Any>>
}
