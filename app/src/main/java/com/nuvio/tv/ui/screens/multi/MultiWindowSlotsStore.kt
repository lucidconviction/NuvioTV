package com.robbdeeze.nuviotv.ui.screens.multi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.slotsDataStore by preferencesDataStore(name = "multi_window_slots")

@Serializable
data class PersistedSlot(
    val index: Int,
    val channelId: String,
    val channelName: String,
    val channelUrl: String,
    val channelLogo: String? = null,
    val channelCategory: String? = null
)

class MultiWindowSlotsStore(private val context: Context) {

    companion object {
        private val SLOTS_KEY = stringPreferencesKey("slots")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val savedSlots: Flow<List<PersistedSlot>> = context.slotsDataStore.data.map { prefs ->
        val raw = prefs[SLOTS_KEY] ?: "[]"
        try {
            json.decodeFromString<List<PersistedSlot>>(raw)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun saveSlots(streams: List<WindowStream>) {
        context.slotsDataStore.edit { prefs ->
            val slots = streams.map { ws ->
                PersistedSlot(
                    index = ws.slotIndex,
                    channelId = ws.channel.id,
                    channelName = ws.channel.name,
                    channelUrl = ws.channel.url,
                    channelLogo = ws.channel.logoUrl,
                    channelCategory = ws.channel.categoryName
                )
            }
            prefs[SLOTS_KEY] = json.encodeToString(slots)
        }
    }

    suspend fun clearSlots() {
        context.slotsDataStore.edit { prefs ->
            prefs.remove(SLOTS_KEY)
        }
    }

    fun toChannels(persisted: List<PersistedSlot>): List<IptvChannel> = persisted.map {
        IptvChannel(
            id = it.channelId,
            name = it.channelName,
            url = it.channelUrl,
            logoUrl = it.channelLogo,
            categoryName = it.channelCategory
        )
    }
}
