package com.robbdeeze.nuviotv.domain.repository

import com.robbdeeze.nuviotv.domain.model.MusicAlbum
import com.robbdeeze.nuviotv.domain.model.MusicNutzCategory
import com.robbdeeze.nuviotv.domain.model.MusicTrack

interface MusicNutzRepository {
    suspend fun getTracksByCategory(category: MusicNutzCategory, page: Int): List<MusicTrack>
    suspend fun searchTracks(query: String, page: Int): List<MusicTrack>
    suspend fun getAlbumsByCategory(category: MusicNutzCategory, page: Int): List<MusicAlbum>
    suspend fun searchAlbums(query: String, page: Int): List<MusicAlbum>
    suspend fun getAlbumTracks(albumId: Long): List<MusicTrack>
    suspend fun resolveYoutubeId(title: String, artist: String): String?
}
