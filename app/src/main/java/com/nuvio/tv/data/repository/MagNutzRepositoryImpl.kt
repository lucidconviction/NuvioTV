package com.robbdeeze.nuviotv.data.repository

import android.content.Context
import android.util.Log
import com.robbdeeze.nuviotv.core.torrent.TorrServerApi
import com.robbdeeze.nuviotv.core.torrent.TorrServerBinary
import com.robbdeeze.nuviotv.domain.model.TorrentItem
import com.robbdeeze.nuviotv.domain.model.TorrentStatus
import com.robbdeeze.nuviotv.domain.repository.MagNutzRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MagNutzRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val binary: TorrServerBinary,
    private val api: TorrServerApi,
) : MagNutzRepository {

    companion object {
        private const val TAG = "MagNutzRepo"
        private const val PREFS_NAME = "magnutz_prefs"
        private const val KEY_TORRENTS = "saved_torrents"
        private const val POLL_INTERVAL_MS = 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _torrents = MutableStateFlow<List<TorrentItem>>(loadPersistedTorrents())
    override val torrents: StateFlow<List<TorrentItem>> = _torrents.asStateFlow()

    private val _torrentUpdates = MutableSharedFlow<TorrentItem>(replay = 0, extraBufferCapacity = 16)
    override val torrentUpdates: SharedFlow<TorrentItem> = _torrentUpdates.asSharedFlow()

    private var pollingJob: Job? = null

    override suspend fun startTorrServer() {
        binary.start()
    }

    override suspend fun stopTorrServer() {
        stopPolling()
        binary.stop()
    }

    override fun isServerRunning(): Boolean = binary.isRunning()

    override suspend fun addMagnet(magnetUri: String): String? {
        val hash = api.addTorrent(magnetUri)
        if (hash != null) {
            val name = extractNameFromMagnet(magnetUri) ?: hash.take(12)
            val item = TorrentItem(
                infoHash = hash,
                name = name,
                magnetUri = magnetUri,
                status = TorrentStatus.DOWNLOADING,
            )
            appendTorrent(item)
        }
        return hash
    }

    override suspend fun addMagnetWithName(magnetUri: String, name: String): String? {
        val hash = api.addTorrent(magnetUri, name)
        if (hash != null) {
            val item = TorrentItem(
                infoHash = hash,
                name = name,
                magnetUri = magnetUri,
                status = TorrentStatus.DOWNLOADING,
            )
            appendTorrent(item)
        }
        return hash
    }

    override suspend fun pauseTorrent(torrentId: String) {
        val item = _torrents.value.find { it.id == torrentId }
        if (item != null) {
            withContext(Dispatchers.IO) {
                api.dropTorrent(item.infoHash)
            }
        }
        updateTorrent(torrentId) { it.copy(status = TorrentStatus.PAUSED) }
        persistTorrents()
    }

    override suspend fun resumeTorrent(torrentId: String) {
        val item = _torrents.value.find { it.id == torrentId }
        if (item != null) {
            withContext(Dispatchers.IO) {
                api.addTorrent(item.magnetUri)
            }
        }
        updateTorrent(torrentId) { it.copy(status = TorrentStatus.DOWNLOADING) }
        persistTorrents()
    }

    override suspend fun removeTorrent(torrentId: String) {
        val item = _torrents.value.find { it.id == torrentId }
        if (item != null) {
            withContext(Dispatchers.IO) {
                api.dropTorrent(item.infoHash)
            }
        }
        _torrents.value = _torrents.value.filter { it.id != torrentId }
        persistTorrents()
    }

    override suspend fun refreshStats(torrentId: String) {
        val item = _torrents.value.find { it.id == torrentId } ?: return
        if (item.status == TorrentStatus.PAUSED || item.status == TorrentStatus.COMPLETED) return
        try {
            val stats = api.getTorrentStats(item.infoHash)
            if (stats != null) {
                val totalSize = stats.torrentSize
                val loaded = stats.loadedSize
                val progress = if (totalSize > 0) (loaded.toFloat() / totalSize).coerceIn(0f, 1f) else 0f
                val isDone = totalSize > 0 && loaded >= totalSize
                val newStatus = when {
                    isDone -> TorrentStatus.COMPLETED
                    else -> TorrentStatus.DOWNLOADING
                }
                val updated = item.copy(
                    progress = progress,
                    downloadSpeed = stats.downloadSpeed,
                    uploadSpeed = stats.uploadSpeed,
                    peers = stats.peers,
                    seeds = stats.seeds,
                    loadedSize = loaded,
                    torrentSize = totalSize,
                    activeTrackers = stats.files.size,
                    status = newStatus,
                )
                replaceTorrent(updated)
                _torrentUpdates.tryEmit(updated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "refreshStats error for ${item.infoHash}", e)
        }
    }

    override fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                val active = _torrents.value.filter {
                    it.status == TorrentStatus.DOWNLOADING || it.status == TorrentStatus.SEEDING
                }
                for (item in active) {
                    refreshStats(item.id)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun appendTorrent(item: TorrentItem) {
        _torrents.value = _torrents.value + item
        persistTorrents()
    }

    private fun replaceTorrent(item: TorrentItem) {
        _torrents.value = _torrents.value.map { if (it.id == item.id) item else it }
        persistTorrents()
    }

    private fun updateTorrent(torrentId: String, transform: (TorrentItem) -> TorrentItem) {
        _torrents.value = _torrents.value.map { if (it.id == torrentId) transform(it) else it }
        persistTorrents()
    }

    private fun extractNameFromMagnet(magnetUri: String): String? {
        val dnMatch = Regex("[&?]dn=([^&]+)").find(magnetUri)
        return dnMatch?.groupValues?.get(1)?.replace("+", " ")
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
    }

    private fun loadPersistedTorrents(): List<TorrentItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TORRENTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                TorrentItem(
                    id = obj.optString("id", ""),
                    infoHash = obj.optString("infoHash", ""),
                    name = obj.optString("name", ""),
                    magnetUri = obj.optString("magnetUri", ""),
                    status = try { TorrentStatus.valueOf(obj.optString("status", "QUEUED")) } catch (_: Exception) { TorrentStatus.QUEUED },
                    progress = obj.optDouble("progress", 0.0).toFloat(),
                    downloadSpeed = obj.optLong("downloadSpeed", 0),
                    uploadSpeed = obj.optLong("uploadSpeed", 0),
                    peers = obj.optInt("peers", 0),
                    seeds = obj.optInt("seeds", 0),
                    loadedSize = obj.optLong("loadedSize", 0),
                    torrentSize = obj.optLong("torrentSize", 0),
                    activeTrackers = obj.optInt("activeTrackers", 0),
                    savePath = obj.optString("savePath", ""),
                    addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load persisted torrents", e)
            emptyList()
        }
    }

    private fun persistTorrents() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        _torrents.value.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("infoHash", item.infoHash)
            obj.put("name", item.name)
            obj.put("magnetUri", item.magnetUri)
            obj.put("status", item.status.name)
            obj.put("progress", item.progress.toDouble())
            obj.put("downloadSpeed", item.downloadSpeed)
            obj.put("uploadSpeed", item.uploadSpeed)
            obj.put("peers", item.peers)
            obj.put("seeds", item.seeds)
            obj.put("loadedSize", item.loadedSize)
            obj.put("torrentSize", item.torrentSize)
            obj.put("activeTrackers", item.activeTrackers)
            obj.put("savePath", item.savePath)
            obj.put("addedAt", item.addedAt)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TORRENTS, arr.toString()).apply()
    }
}
