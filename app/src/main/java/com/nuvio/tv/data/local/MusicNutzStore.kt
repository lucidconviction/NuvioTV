package com.robbdeeze.nuviotv.data.local

import android.content.Context
import com.robbdeeze.nuviotv.domain.model.MusicDownloadedTrack
import com.robbdeeze.nuviotv.domain.model.MusicPlaylist
import com.robbdeeze.nuviotv.domain.model.MusicTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicNutzStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun loadPlaylists(): List<MusicPlaylist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val decoded = json.decodeFromString<PersistedPlaylists>(raw)
            decoded.playlists.map { it.toDomain() }
        } catch (e: Exception) { emptyList() }
    }

    fun savePlaylists(playlists: List<MusicPlaylist>) {
        val persisted = PersistedPlaylists(playlists.map { it.toPersisted() })
        prefs.edit().putString(KEY_PLAYLISTS, json.encodeToString(persisted)).apply()
    }

    fun loadSavedAlbumIds(): Set<Long> {
        val raw = prefs.getString(KEY_SAVED_ALBUMS, null) ?: return emptySet()
        return try {
            val decoded = json.decodeFromString<PersistedAlbumIds>(raw)
            decoded.ids.toSet()
        } catch (e: Exception) { emptySet() }
    }

    fun saveSavedAlbumIds(ids: Set<Long>) {
        val persisted = PersistedAlbumIds(ids.toList())
        prefs.edit().putString(KEY_SAVED_ALBUMS, json.encodeToString(persisted)).apply()
    }

    fun loadDownloadedTracks(): List<MusicDownloadedTrack> {
        val raw = prefs.getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return try {
            val decoded = json.decodeFromString<PersistedDownloads>(raw)
            decoded.tracks.map { it.toDomain() }
        } catch (e: Exception) { emptyList() }
    }

    fun saveDownloadedTracks(tracks: List<MusicDownloadedTrack>) {
        val persisted = PersistedDownloads(tracks.map { it.toPersisted() })
        prefs.edit().putString(KEY_DOWNLOADS, json.encodeToString(persisted)).apply()
    }

    suspend fun ensureDownloadDir(): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "music")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    @Serializable
    private data class PersistedPlaylists(val playlists: List<PersistedPlaylist> = emptyList())

    @Serializable
    private data class PersistedPlaylist(
        val id: String = "",
        val name: String = "",
        val trackIds: List<Long> = emptyList(),
        val createdAt: Long = 0,
    )

    @Serializable
    private data class PersistedAlbumIds(val ids: List<Long> = emptyList())

    @Serializable
    private data class PersistedDownloads(val tracks: List<PersistedDownload> = emptyList())

    @Serializable
    private data class PersistedDownload(
        val trackId: Long = 0,
        val title: String = "",
        val artistName: String = "",
        val albumCover: String = "",
        val localPath: String = "",
        val downloadedAt: Long = 0,
    )

    private fun MusicPlaylist.toPersisted() = PersistedPlaylist(
        id = id, name = name,
        trackIds = tracks.map { it.id },
        createdAt = createdAt,
    )

    private fun PersistedPlaylist.toDomain(): MusicPlaylist {
        return MusicPlaylist(
            id = id, name = name, tracks = emptyList(), createdAt = createdAt,
        )
    }

    private fun MusicDownloadedTrack.toPersisted() = PersistedDownload(
        trackId = trackId, title = title, artistName = artistName,
        albumCover = albumCover, localPath = localPath, downloadedAt = downloadedAt,
    )

    private fun PersistedDownload.toDomain() = MusicDownloadedTrack(
        trackId = trackId, title = title, artistName = artistName,
        albumCover = albumCover, localPath = localPath, downloadedAt = downloadedAt,
    )

    companion object {
        private const val PREFS_NAME = "musicnutz_store"
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_SAVED_ALBUMS = "saved_albums"
        private const val KEY_DOWNLOADS = "downloaded_tracks"
    }
}
