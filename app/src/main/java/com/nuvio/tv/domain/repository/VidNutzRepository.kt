package com.robbdeeze.nuviotv.domain.repository

import com.robbdeeze.nuviotv.domain.model.VidNutzCategory
import com.robbdeeze.nuviotv.domain.model.VidNutzVideo

interface VidNutzRepository {
    suspend fun getVideosByCategory(category: VidNutzCategory, page: Int): List<VidNutzVideo>
    suspend fun searchVideos(query: String, page: Int): List<VidNutzVideo>
}
