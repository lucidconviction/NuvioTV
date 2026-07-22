package com.robbdeeze.nuviotv.domain.repository

import com.robbdeeze.nuviotv.domain.model.TorrentItem
import com.robbdeeze.nuviotv.domain.model.TorrentStatus
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MagNutzRepository {

    val torrents: StateFlow<List<TorrentItem>>

    val torrentUpdates: SharedFlow<TorrentItem>

    suspend fun addMagnet(magnetUri: String): String?

    suspend fun addMagnetWithName(magnetUri: String, name: String): String?

    suspend fun pauseTorrent(torrentId: String)

    suspend fun resumeTorrent(torrentId: String)

    suspend fun removeTorrent(torrentId: String)

    suspend fun refreshStats(torrentId: String)

    suspend fun startTorrServer()

    suspend fun stopTorrServer()

    fun isServerRunning(): Boolean

    fun startPolling()

    fun stopPolling()
}
