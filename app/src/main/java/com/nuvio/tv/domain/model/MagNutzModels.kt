package com.robbdeeze.nuviotv.domain.model

import java.util.UUID

enum class TorrentStatus {
    DOWNLOADING, SEEDING, COMPLETED, FAILED, PAUSED, QUEUED
}

data class TorrentItem(
    val id: String = UUID.randomUUID().toString(),
    val infoHash: String,
    val name: String,
    val magnetUri: String,
    val status: TorrentStatus = TorrentStatus.QUEUED,
    val progress: Float = 0f,
    val downloadSpeed: Long = 0,
    val uploadSpeed: Long = 0,
    val peers: Int = 0,
    val seeds: Int = 0,
    val loadedSize: Long = 0,
    val torrentSize: Long = 0,
    val activeTrackers: Int = 0,
    val savePath: String = "",
    val errorMessage: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

data class TorrentUiState(
    val torrents: List<TorrentItem> = emptyList(),
    val filter: TorrentStatus? = null,
    val selectedTorrent: TorrentItem? = null,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val magnetInput: String = "",
    val errorMessage: String? = null,
)
