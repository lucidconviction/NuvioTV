package com.nuvio.tv.domain.repository

import com.nuvio.tv.domain.model.MusicAlbum
import com.nuvio.tv.domain.model.MusicNutzCategory
import com.nuvio.tv.domain.model.MusicTrack

interface MusicNutzRepository {
    suspend fun getTracksByCategory(category: MusicNutzCategory, page: Int): List<MusicTrack>
    suspend fun searchTracks(query: String, page: Int): List<MusicTrack>
    suspend fun getAlbumsByCategory(category: MusicNutzCategory, page: Int): List<MusicAlbum>
    suspend fun searchAlbums(query: String, page: Int): List<MusicAlbum>
    suspend fun getAlbumTracks(albumId: Long): List<MusicTrack>
    suspend fun resolveYoutubeId(title: String, artist: String): String?
}
