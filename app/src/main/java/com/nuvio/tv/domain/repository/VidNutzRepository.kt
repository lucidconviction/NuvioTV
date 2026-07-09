package com.nuvio.tv.domain.repository

import com.nuvio.tv.domain.model.VidNutzCategory
import com.nuvio.tv.domain.model.VidNutzVideo

interface VidNutzRepository {
    suspend fun getVideosByCategory(category: VidNutzCategory, page: Int): List<VidNutzVideo>
    suspend fun searchVideos(query: String, page: Int): List<VidNutzVideo>
}
