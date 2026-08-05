package com.robbdeeze.nuviotv.data.local

import com.robbdeeze.nuviotv.domain.model.TeleNutzVideo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
class TeleNutzStore(private val storage: TeleNutzStorage) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private var bookmarks: MutableList<TeleNutzVideo> = mutableListOf()
    private var downloads: MutableList<TeleNutzVideo> = mutableListOf()
    private var recentSearches: MutableList<String> = mutableListOf()
    private var loaded = false

    fun getRecentSearches(): List<String> {
        ensureLoaded()
        return recentSearches.toList()
    }

    fun addRecentSearch(query: String) {
        ensureLoaded()
        recentSearches.remove(query)
        recentSearches.add(0, query)
        if (recentSearches.size > 10) recentSearches.removeAt(recentSearches.lastIndex)
        persist()
    }

    fun clearRecentSearches() {
        ensureLoaded()
        recentSearches.clear()
        persist()
    }

    fun getBookmarks(): List<TeleNutzVideo> {
        ensureLoaded()
        return bookmarks.toList()
    }

    fun getDownloads(): List<TeleNutzVideo> {
        ensureLoaded()
        return downloads.toList()
    }

    fun isDownloaded(id: Long, chatId: Long): Boolean {
        ensureLoaded()
        return downloads.any { it.id == id && it.chatId == chatId && it.isDownloaded }
    }

    fun toggleBookmark(video: TeleNutzVideo): Boolean {
        ensureLoaded()
        val index = bookmarks.indexOfFirst { it.id == video.id && it.chatId == video.chatId }
        val nowBookmarked = if (index >= 0) {
            bookmarks.removeAt(index)
            false
        } else {
            bookmarks.add(video.copy(isBookmarked = true))
            true
        }
        persist()
        return nowBookmarked
    }

    fun saveDownload(video: TeleNutzVideo) {
        ensureLoaded()
        val index = downloads.indexOfFirst { it.id == video.id && it.chatId == video.chatId }
        if (index >= 0) {
            downloads[index] = video
        } else {
            downloads.add(video)
        }
        persist()
    }

    fun removeDownload(id: Long, chatId: Long): TeleNutzVideo? {
        ensureLoaded()
        val index = downloads.indexOfFirst { it.id == id && it.chatId == chatId }
        return if (index >= 0) {
            val removed = downloads.removeAt(index)
            persist()
            removed
        } else null
    }

    // ── Repository-facing API (aliases/overloads) ─────────────────────────

    fun getBookmarkedVideos(): List<TeleNutzVideo> = getBookmarks()

    fun isBookmarked(chatId: Long, msgId: Long): Boolean {
        ensureLoaded()
        return bookmarks.any { it.id == msgId && it.chatId == chatId }
    }

    fun markDownloaded(video: TeleNutzVideo) {
        saveDownload(video.copy(isDownloaded = true))
    }

    fun getSearchHistory(): List<String> = getRecentSearches()

    fun addSearchQuery(query: String): String {
        addRecentSearch(query)
        return query
    }

    fun clearSearchHistory() = clearRecentSearches()

    private fun ensureLoaded() {
        if (loaded) return
        try {
            val bookmarksJson = storage.loadBookmarksPayload()
            if (!bookmarksJson.isNullOrBlank()) {
                bookmarks = json.decodeFromString<List<TeleNutzVideo>>(bookmarksJson).toMutableList()
            }
            val downloadsJson = storage.loadDownloadsPayload()
            if (!downloadsJson.isNullOrBlank()) {
                downloads = json.decodeFromString<List<TeleNutzVideo>>(downloadsJson).toMutableList()
            }
            val searchesJson = storage.loadRecentSearchesPayload()
            if (!searchesJson.isNullOrBlank()) {
                recentSearches = json.decodeFromString<List<String>>(searchesJson).toMutableList()
            }
        } catch (_: Exception) {}
        loaded = true
    }

    private fun persist() {
        try {
            storage.saveBookmarksPayload(json.encodeToString(bookmarks.toList()))
            storage.saveDownloadsPayload(json.encodeToString(downloads.toList()))
            storage.saveRecentSearchesPayload(json.encodeToString(recentSearches.toList()))
        } catch (_: Exception) {}
    }
}
