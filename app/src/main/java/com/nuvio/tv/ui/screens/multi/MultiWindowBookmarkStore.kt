package com.robbdeeze.nuviotv.ui.screens.multi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.robbdeeze.nuviotv.domain.model.IptvChannel

private val Context.bookmarkDataStore by preferencesDataStore(name = "multi_window_bookmarks")

@Serializable
data class BookmarkedSlot(
    val slotIndex: Int,
    val channelId: String,
    val channelName: String,
    val channelUrl: String,
    val channelLogo: String
)

@Serializable
data class MultiWindowBookmark(
    val name: String,
    val layoutName: String,
    val slots: List<BookmarkedSlot>,
    val createdAt: Long = System.currentTimeMillis()
)

class MultiWindowBookmarkStore(private val context: Context) {

    companion object {
        private val BOOKMARKS_KEY = stringPreferencesKey("bookmarks")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val bookmarks: Flow<List<MultiWindowBookmark>> = context.bookmarkDataStore.data.map { prefs ->
        val raw = prefs[BOOKMARKS_KEY] ?: "[]"
        try {
            json.decodeFromString<List<MultiWindowBookmark>>(raw)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun save(name: String, layout: MultiWindowLayout) {
        val slots = MultiWindowStore.streams.map { stream ->
            BookmarkedSlot(
                slotIndex = stream.slotIndex,
                channelId = stream.channel.id,
                channelName = stream.channel.name,
                channelUrl = stream.channel.url,
                channelLogo = stream.channel.logoUrl ?: ""
            )
        }
        context.bookmarkDataStore.edit { prefs ->
            val raw = prefs[BOOKMARKS_KEY] ?: "[]"
            val list = try {
                json.decodeFromString<List<MultiWindowBookmark>>(raw).toMutableList()
            } catch (_: Exception) { mutableListOf() }
            list.add(MultiWindowBookmark(name = name, layoutName = layout.name, slots = slots, createdAt = System.currentTimeMillis()))
            prefs[BOOKMARKS_KEY] = json.encodeToString(list)
        }
    }

    suspend fun restore(bookmark: MultiWindowBookmark) {
        val layout = MultiWindowLayout.entries.find { it.name == bookmark.layoutName }
        MultiWindowStore.clear()
        bookmark.slots.forEach { slot ->
            val channel = IptvChannel(
                id = slot.channelId,
                name = slot.channelName,
                url = slot.channelUrl,
                logoUrl = slot.channelLogo.ifEmpty { null }
            )
            MultiWindowStore.addToSlot(channel, slot.slotIndex)
        }
        if (layout != null) {
            MultiWindowStore.setLayout(layout)
        }
    }

    suspend fun delete(bookmark: MultiWindowBookmark) {
        context.bookmarkDataStore.edit { prefs ->
            val raw = prefs[BOOKMARKS_KEY] ?: "[]"
            val list = try {
                json.decodeFromString<List<MultiWindowBookmark>>(raw).toMutableList()
            } catch (_: Exception) { mutableListOf() }
            list.removeAll { it.createdAt == bookmark.createdAt }
            prefs[BOOKMARKS_KEY] = json.encodeToString(list)
        }
    }
}
