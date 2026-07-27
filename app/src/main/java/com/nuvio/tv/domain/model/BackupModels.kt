package com.robbdeeze.nuviotv.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val appName: String = "RNutz NuvioTV",

    val iptvSources: List<BackupIptvSource> = emptyList(),
    val iptvFavorites: List<String> = emptyList(),
    val iptvHistory: List<BackupChannelHistory> = emptyList(),

    val settings: BackupSettings = BackupSettings(),

    val auth: BackupAuth = BackupAuth(),

    val traktTokens: BackupTraktTokens? = null,

    val profiles: List<BackupProfile> = emptyList(),

    val collections: List<BackupCollection> = emptyList(),

    val watchProgress: List<BackupWatchProgress> = emptyList(),
    val watchedItems: List<BackupWatchedItem> = emptyList(),

    val multiWindowBookmarks: List<BackupMultiWindowBookmark> = emptyList(),

    val magnutzTorrents: List<BackupTorrent> = emptyList(),

    val musicNutzPlaylists: List<BackupMusicPlaylist> = emptyList(),
    val musicNutzSavedAlbums: List<Long> = emptyList(),
    val musicNutzDownloads: List<BackupMusicDownload> = emptyList(),

    val addonUrls: List<String> = emptyList(),
    val addonEnabledStates: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class BackupIptvSource(
    val name: String,
    val url: String,
    val type: String,
)

@Serializable
data class BackupChannelHistory(
    val channelId: String,
    val channelName: String,
    val channelUrl: String,
    val logoUrl: String? = null,
    val lastWatchedAt: Long = 0,
)

@Serializable
data class BackupSettings(
    val theme: String = "",
    val layout: String = "",
    val playback: String = "",
    val experienceMode: String = "",
)

@Serializable
data class BackupAuth(
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val debridTokens: Map<String, String> = emptyMap(),
)

@Serializable
data class BackupTraktTokens(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0,
)

@Serializable
data class BackupProfile(
    val name: String = "",
    val avatarUrl: String? = null,
    val isPrimary: Boolean = false,
)

@Serializable
data class BackupCollection(
    val id: String = "",
    val name: String = "",
    val items: List<String> = emptyList(),
)

@Serializable
data class BackupWatchProgress(
    val contentId: String = "",
    val contentType: String = "",
    val progress: Float = 0f,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupWatchedItem(
    val contentId: String = "",
    val contentType: String = "",
    val watchedAt: Long = 0,
)

@Serializable
data class BackupMultiWindowBookmark(
    val name: String = "",
    val layoutName: String = "",
    val slots: List<BackupBookmarkedSlot> = emptyList(),
)

@Serializable
data class BackupBookmarkedSlot(
    val slotIndex: Int = 0,
    val channelId: String = "",
    val channelName: String = "",
    val channelUrl: String = "",
    val channelLogo: String? = null,
)

@Serializable
data class BackupTorrent(
    val infoHash: String = "",
    val name: String = "",
    val magnetUri: String = "",
    val savePath: String = "",
)

@Serializable
data class BackupMusicPlaylist(
    val id: String = "",
    val name: String = "",
    val trackIds: List<Long> = emptyList(),
    val createdAt: Long = 0,
)

@Serializable
data class BackupMusicDownload(
    val trackId: Long = 0,
    val title: String = "",
    val artistName: String = "",
    val albumCover: String = "",
    val localPath: String = "",
)
