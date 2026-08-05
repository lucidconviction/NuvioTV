package com.robbdeeze.nuviotv.data.repository

import com.robbdeeze.nuviotv.data.local.TeleNutzStore
import com.robbdeeze.nuviotv.domain.model.TdMessage
import com.robbdeeze.nuviotv.domain.model.TeleNutzPlayerLaunch
import com.robbdeeze.nuviotv.domain.model.TeleNutzVideo

object TeleNutzRepository {

    lateinit var engine: TelegramTdEngine
        private set
    lateinit var store: TeleNutzStore
        private set

    @Volatile
    var lastSearchQuery: String = ""
        private set

    @Volatile
    var lastSearchResults: List<TeleNutzVideo> = emptyList()
        private set

    fun init(tdEngine: TelegramTdEngine, storage: TeleNutzStore) {
        engine = tdEngine
        store = storage
    }

    suspend fun start() { engine.start() }
    suspend fun close() { engine.close() }
    fun getAuthInfo() = engine.getAuthInfo()
    suspend fun setPhoneNumber(phone: String) { engine.setPhoneNumber(phone) }
    suspend fun checkAuthCode(code: String) { engine.checkAuthCode(code) }
    suspend fun checkPassword(password: String) { engine.checkPassword(password) }
    suspend fun requestQrCode() { engine.requestQrCode() }

    fun getBookmarkedVideos(): List<TeleNutzVideo> = store.getBookmarkedVideos()
    fun isBookmarked(chatId: Long, msgId: Long): Boolean = store.isBookmarked(chatId, msgId)
    fun toggleBookmark(video: TeleNutzVideo) { store.toggleBookmark(video) }
    fun getDownloads(): List<TeleNutzVideo> = store.getDownloads()
    fun getDownloadedVideos(): List<TeleNutzVideo> = store.getDownloads()
    fun isDownloaded(chatId: Long, msgId: Long): Boolean = store.isDownloaded(chatId, msgId)
    fun markDownloaded(video: TeleNutzVideo) { store.markDownloaded(video) }
    fun removeDownload(video: TeleNutzVideo) { store.removeDownload(video.id, video.chatId) }
    fun getSearchHistory(): List<String> = store.getSearchHistory()
    fun addSearchQuery(query: String) { store.addSearchQuery(query).also { lastSearchQuery = query } }
    fun clearSearchHistory() { store.clearSearchHistory() }
    suspend fun resolveVideoPlayback(video: TeleNutzVideo): TeleNutzPlayerLaunch? = engine.resolvePlayback(video)
    fun getMessages(chatId: Long, limit: Int = 30): List<TdMessage> = engine.getMessages(chatId, limit)
    fun loadMoreMessages(chatId: Long) { engine.loadMoreMessages(chatId) }
    fun searchMessages(chatId: Long, query: String) { engine.searchMessages(chatId, query) }
    fun startFileDownload(fileId: Int, video: TeleNutzVideo) { engine.downloadFile(fileId, video) }
    suspend fun searchVideos(query: String): List<TeleNutzVideo> {
        lastSearchQuery = query
        store.addSearchQuery(query)
        val messages = engine.searchVideoMessages(query)
        return messages.map { it.toTeleNutzVideo() }
    }
    suspend fun downloadVideo(video: TeleNutzVideo, onProgress: ((Float) -> Unit)? = null): String? {
        return engine.downloadVideoFile(video.fileId, onProgress)
    }
    suspend fun deleteDownload(video: TeleNutzVideo) {
        val localPath = video.localPath
        if (!localPath.isNullOrBlank()) {
            engine.deleteVideoFile(localPath)
        }
        store.removeDownload(video.id, video.chatId)
    }
    suspend fun cancelDownload(video: TeleNutzVideo) {
        engine.cancelDownload(video.fileId)
    }
    fun getFileProgress(fileId: Int): Float {
        val state = engine.peekFileDownloadState(fileId)
        return if (state != null && state.totalSize > 0L) state.downloadedSize.toFloat() / state.totalSize.toFloat() else 0f
    }
}

private fun TdMessage.toTeleNutzVideo(): TeleNutzVideo = TeleNutzVideo(
    id = id,
    chatId = chatId,
    chatTitle = chatTitle,
    text = text,
    date = date,
    thumbnailUrl = thumbnailUrl,
    fileId = fileId,
    localPath = localPath,
    fileSize = fileSize,
)
