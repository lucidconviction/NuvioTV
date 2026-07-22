@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.robbdeeze.nuviotv.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.robbdeeze.nuviotv.data.local.ChannelHistoryStore
import com.robbdeeze.nuviotv.data.local.MusicNutzStore
import com.robbdeeze.nuviotv.domain.model.BackupChannelHistory
import com.robbdeeze.nuviotv.domain.model.BackupData
import com.robbdeeze.nuviotv.domain.model.BackupIptvSource
import com.robbdeeze.nuviotv.domain.model.BackupMusicDownload
import com.robbdeeze.nuviotv.domain.model.BackupMusicPlaylist
import com.robbdeeze.nuviotv.domain.model.BackupMultiWindowBookmark
import com.robbdeeze.nuviotv.domain.model.BackupBookmarkedSlot
import com.robbdeeze.nuviotv.domain.model.BackupTorrent
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.IptvSource
import com.robbdeeze.nuviotv.domain.model.MusicPlaylist
import com.robbdeeze.nuviotv.domain.repository.IptvRepository
import com.robbdeeze.nuviotv.domain.repository.MagNutzRepository
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowBookmarkStore
import com.robbdeeze.nuviotv.ui.theme.NuvioTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupEntryPoint {
    fun iptvRepository(): IptvRepository
    fun magNutzRepository(): MagNutzRepository
    fun musicNutzStore(): MusicNutzStore
    fun channelHistoryStore(): ChannelHistoryStore
}

@Composable
fun BackupRestoreScreen(
    onBackPress: () -> Unit = {}
) {
    BackHandler { onBackPress() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BackupEntryPoint::class.java)
    }
    val iptvRepo = remember { entryPoint.iptvRepository() }
    val magNutzRepo = remember { entryPoint.magNutzRepository() }
    val musicStore = remember { entryPoint.musicNutzStore() }
    val historyStore = remember { entryPoint.channelHistoryStore() }

    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }

    suspend fun buildBackupData(): BackupData = withContext(Dispatchers.IO) {
        val sources = iptvRepo.getSources().first()
        val favorites = iptvRepo.getFavorites().first().map { it.id }
        val history = historyStore.historyFlow.first().map {
            BackupChannelHistory(it.id, it.name, it.url, it.logoUrl, System.currentTimeMillis())
        }
        val bookmarkStore = MultiWindowBookmarkStore(context)
        val bookmarks = bookmarkStore.bookmarks.first().map { bm ->
            BackupMultiWindowBookmark(
                name = bm.name, layoutName = bm.layoutName,
                slots = bm.slots.map { s -> BackupBookmarkedSlot(s.slotIndex, s.channelName, s.channelUrl, s.channelLogo) }
            )
        }
        val torrents = magNutzRepo.torrents.first().map { t ->
            BackupTorrent(t.infoHash, t.name, t.magnetUri, t.savePath)
        }
        val playlists = musicStore.loadPlaylists().map { p ->
            BackupMusicPlaylist(p.id, p.name, p.tracks.map { it.id }, p.createdAt)
        }
        val savedAlbums = musicStore.loadSavedAlbumIds().toList()
        val downloads = musicStore.loadDownloadedTracks().map { d ->
            BackupMusicDownload(d.trackId, d.title, d.artistName, d.localPath)
        }
        BackupData(
            iptvSources = sources.map { BackupIptvSource(it.name, it.url, it.type) },
            iptvFavorites = favorites,
            iptvHistory = history,
            multiWindowBookmarks = bookmarks,
            magnutzTorrents = torrents,
            musicNutzPlaylists = playlists,
            musicNutzSavedAlbums = savedAlbums,
            musicNutzDownloads = downloads,
        )
    }

    suspend fun restoreBackup(data: BackupData): List<String> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<String>()
        if (data.iptvSources.isNotEmpty()) {
            data.iptvSources.forEach { src ->
                try { iptvRepo.addSource(IptvSource(name = src.name, url = src.url, type = src.type)) }
                catch (_: Exception) {}
            }
            imported.add("iptv_sources")
        }
        if (data.iptvFavorites.isNotEmpty()) {
            val existing = iptvRepo.getFavorites().first().map { it.id }.toSet()
            val allSources = iptvRepo.getSources().first()
            for (source in allSources) {
                val channels = iptvRepo.getChannels(source)
                for (ch in channels) {
                    if (ch.id in data.iptvFavorites && ch.id !in existing) {
                        try { iptvRepo.addFavorite(ch) } catch (_: Exception) {}
                    }
                }
            }
            imported.add("iptv_favorites")
        }
        if (data.musicNutzPlaylists.isNotEmpty()) {
            val playlists = data.musicNutzPlaylists.map { MusicPlaylist(id = it.id, name = it.name, createdAt = it.createdAt) }
            val existing = musicStore.loadPlaylists().toMutableList()
            existing.addAll(playlists)
            musicStore.savePlaylists(existing)
            imported.add("music_playlists")
        }
        if (data.musicNutzSavedAlbums.isNotEmpty()) {
            musicStore.saveSavedAlbumIds(data.musicNutzSavedAlbums.toSet())
            imported.add("music_saved_albums")
        }
        imported
    }

    fun exportToClipboard(data: BackupData) {
        val text = json.encodeToString(data)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NuvioTV Backup", text))
    }

    fun parseFromClipboard(): BackupData? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val text = clip.getItemAt(0).text?.toString() ?: return null
        return try { json.decodeFromString<BackupData>(text) } catch (_: Exception) { null }
    }

    val fileExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                try {
                    val data = buildBackupData()
                    val text = json.encodeToString(data)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    Toast.makeText(context, "Backup saved to file", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                isExporting = false
            }
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                try {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    val data = json.decodeFromString<BackupData>(text)
                    val sections = restoreBackup(data)
                    Toast.makeText(context, "Restored sections: ${sections.joinToString(", ")}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                isImporting = false
            }
        }
    }

    SettingsStandaloneScaffold(
        title = "Backup & Restore",
        subtitle = "Export or import your app data"
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsDetailHeader(
                title = "Backup & Restore",
                subtitle = "Export your IPTV sources, bookmarks, and settings"
            )

            SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
                SettingsActionRow(
                    title = "Export to Clipboard",
                    subtitle = "Copy backup JSON to clipboard",
                    trailingIcon = Icons.Default.ContentCopy,
                    enabled = !isExporting,
                    onClick = {
                        scope.launch {
                            isExporting = true
                            try {
                                val data = buildBackupData()
                                exportToClipboard(data)
                                Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            isExporting = false
                        }
                    }
                )

                SettingsActionRow(
                    title = "Export to File",
                    subtitle = "Save backup as .json file",
                    trailingIcon = Icons.Default.CloudUpload,
                    enabled = !isExporting,
                    onClick = { fileExportLauncher.launch("nuviotv_backup.json") }
                )

                SettingsActionRow(
                    title = "Import from Clipboard",
                    subtitle = "Restore from clipboard backup JSON",
                    trailingIcon = Icons.Default.ContentPaste,
                    enabled = !isImporting,
                    onClick = {
                        scope.launch {
                            isImporting = true
                            try {
                                val data = parseFromClipboard()
                                if (data != null) {
                                    val sections = restoreBackup(data)
                                    Toast.makeText(context, "Restored sections: ${sections.joinToString(", ")}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "No valid backup found in clipboard", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            isImporting = false
                        }
                    }
                )

                SettingsActionRow(
                    title = "Import from File",
                    subtitle = "Restore from .json backup file",
                    trailingIcon = Icons.Default.CloudDownload,
                    enabled = !isImporting,
                    onClick = { fileImportLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }
        }
    }
}
