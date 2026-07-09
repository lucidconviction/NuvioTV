package com.nuvio.tv.data.repository

import com.nuvio.tv.core.network.HttpClient
import com.nuvio.tv.data.youtube.PlatformYouTubeSearch
import com.nuvio.tv.domain.model.MusicAlbum
import com.nuvio.tv.domain.model.MusicNutzCategory
import com.nuvio.tv.domain.model.MusicTrack
import com.nuvio.tv.domain.repository.MusicNutzRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicNutzRepositoryImpl @Inject constructor() : MusicNutzRepository {

    private val BASE = "https://api.deezer.com"
    private val json = Json { ignoreUnknownKeys = true }

    private val categoryQueries = mapOf(
        MusicNutzCategory.ROCK to "rock",
        MusicNutzCategory.HIP_HOP to "hip hop",
        MusicNutzCategory.ELECTRONIC to "electronic",
        MusicNutzCategory.POP to "pop",
        MusicNutzCategory.R_AND_B to "r&b",
        MusicNutzCategory.JAZZ to "jazz",
        MusicNutzCategory.CLASSICAL to "classical",
        MusicNutzCategory.COUNTRY to "country",
        MusicNutzCategory.METAL to "metal",
        MusicNutzCategory.INDIE to "indie",
    )

    override suspend fun getTracksByCategory(category: MusicNutzCategory, page: Int): List<MusicTrack> {
        return when (category) {
            MusicNutzCategory.TRENDING -> fetchTrending(page)
            MusicNutzCategory.NEW_RELEASES -> fetchNewReleases(page)
            else -> {
                val query = categoryQueries[category] ?: return emptyList()
                val encoded = encodeUrl("$query music")
                val index = (page - 1) * 20
                fetchTracks("$BASE/search/track?q=$encoded&limit=20&index=$index")
            }
        }
    }

    override suspend fun searchTracks(query: String, page: Int): List<MusicTrack> {
        if (query.isBlank()) return emptyList()
        val encoded = encodeUrl(query)
        val index = (page - 1) * 20
        return fetchTracks("$BASE/search/track?q=$encoded&limit=20&index=$index")
    }

    override suspend fun getAlbumsByCategory(category: MusicNutzCategory, page: Int): List<MusicAlbum> {
        return when (category) {
            MusicNutzCategory.TRENDING, MusicNutzCategory.NEW_RELEASES -> fetchTrendingAlbums(page)
            else -> {
                val query = categoryQueries[category] ?: return emptyList()
                val encoded = encodeUrl("$query album")
                val index = (page - 1) * 20
                fetchAlbums("$BASE/search/album?q=$encoded&limit=20&index=$index")
            }
        }
    }

    override suspend fun searchAlbums(query: String, page: Int): List<MusicAlbum> {
        if (query.isBlank()) return emptyList()
        val encoded = encodeUrl(query)
        val index = (page - 1) * 20
        return fetchAlbums("$BASE/search/album?q=$encoded&limit=20&index=$index")
    }

    override suspend fun getAlbumTracks(albumId: Long): List<MusicTrack> {
        return fetchTracks("$BASE/album/$albumId/tracks")
    }

    override suspend fun resolveYoutubeId(title: String, artist: String): String? {
        val query = "$title $artist audio"
        return try {
            val results = PlatformYouTubeSearch.search(query)
            results.firstOrNull()?.videoId
        } catch (_: Exception) { null }
    }

    private suspend fun fetchTrending(page: Int): List<MusicTrack> {
        val index = (page - 1) * 20
        return fetchTracks("$BASE/chart/0/tracks?limit=20&index=$index")
    }

    private suspend fun fetchNewReleases(page: Int): List<MusicTrack> {
        val index = (page - 1) * 20
        return withContext(Dispatchers.IO) {
            try {
                val response = httpGetText("$BASE/chart/0/albums?limit=20&index=$index")
                val parsed = json.decodeFromString<DeezerAlbumList>(response)
                parsed.data.mapNotNull { album ->
                    try {
                        val albumResp = httpGetText("$BASE/album/${album.id}/tracks?limit=1")
                        val albumParsed = json.decodeFromString<DeezerTrackList>(albumResp)
                        albumParsed.data.firstOrNull()?.let { track ->
                            MusicTrack(
                                id = track.id, title = track.title, artistName = track.artist.name,
                                albumName = album.title, albumCover = album.cover_medium,
                                durationSeconds = track.duration, previewUrl = track.preview
                            )
                        }
                    } catch (e: Exception) { e.printStackTrace(); null }
                }
            } catch (e: Exception) { e.printStackTrace(); emptyList() }
        }
    }

    private suspend fun fetchTracks(url: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        try {
            val response = httpGetText(url)
            val parsed = json.decodeFromString<DeezerTrackList>(response)
            parsed.data.map { track ->
                MusicTrack(
                    id = track.id, title = track.title, artistName = track.artist.name,
                    albumName = track.album.title, albumCover = track.album.cover_medium,
                    durationSeconds = track.duration, previewUrl = track.preview
                )
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    private suspend fun fetchTrendingAlbums(page: Int): List<MusicAlbum> {
        val index = (page - 1) * 20
        return fetchAlbums("$BASE/chart/0/albums?limit=20&index=$index")
    }

    private suspend fun fetchAlbums(url: String): List<MusicAlbum> = withContext(Dispatchers.IO) {
        try {
            val response = httpGetText(url)
            val parsed = json.decodeFromString<DeezerAlbumList>(response)
            parsed.data.map { album ->
                MusicAlbum(
                    id = album.id, title = album.title, artistName = album.artist.name,
                    coverUrl = album.cover_medium, releaseDate = album.release_date, trackCount = 0
                )
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    private fun encodeUrl(s: String): String {
        return s.replace(" ", "+").replace(",", "%2C").replace("&", "%26").replace("?", "%3F").replace("#", "%23")
    }

    @Serializable
    data class DeezerTrackData(
        val id: Long = 0,
        val title: String = "",
        val duration: Int = 0,
        val preview: String? = null,
        val artist: DeezerArtist = DeezerArtist(),
        val album: DeezerAlbum = DeezerAlbum()
    )

    @Serializable
    data class DeezerArtist(val name: String = "")

    @Serializable
    data class DeezerAlbum(val title: String = "", val cover_medium: String = "")

    @Serializable
    data class DeezerTrackList(val data: List<DeezerTrackData> = emptyList())

    @Serializable
    data class DeezerAlbumData(
        val id: Long = 0,
        val title: String = "",
        val cover_medium: String = "",
        val artist: DeezerArtist = DeezerArtist(),
        val release_date: String = ""
    )

    @Serializable
    data class DeezerAlbumList(val data: List<DeezerAlbumData> = emptyList())

    companion object {
        private suspend fun httpGetText(url: String): String = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${response.message}")
                response.body.string()
            }
        }
    }
}
