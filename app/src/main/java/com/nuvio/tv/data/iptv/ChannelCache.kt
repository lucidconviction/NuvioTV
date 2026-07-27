package com.robbdeeze.nuviotv.data.iptv

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.channelCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "iptv_channel_cache")

@Singleton
class ChannelCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheTtlMs = 3600_000L

    suspend fun get(sourceUrl: String): List<IptvChannel>? {
        val prefs = context.channelCacheDataStore.data.first()
        val raw = prefs[channelsKey(sourceUrl)] ?: return null
        val cachedAt = prefs[cacheTimeKey(sourceUrl)] ?: return null
        if (System.currentTimeMillis() - cachedAt > cacheTtlMs) return null
        return try {
            json.decodeFromString<List<SerializableChannel>>(raw).map { it.toDomain() }
        } catch (_: Exception) { null }
    }

    suspend fun put(sourceUrl: String, channels: List<IptvChannel>) {
        context.channelCacheDataStore.edit { prefs ->
            val serialized = channels.map { it.toSerializable() }
            prefs[channelsKey(sourceUrl)] = json.encodeToString(serialized)
            prefs[cacheTimeKey(sourceUrl)] = System.currentTimeMillis()
        }
    }

    suspend fun invalidate(sourceUrl: String) {
        context.channelCacheDataStore.edit { prefs ->
            prefs.remove(channelsKey(sourceUrl))
            prefs.remove(cacheTimeKey(sourceUrl))
        }
    }

    suspend fun invalidateAll() {
        context.channelCacheDataStore.edit { prefs ->
            val keys = prefs.asMap().keys.filter { it.name.startsWith("channels_") || it.name.startsWith("cache_time_") }
            keys.forEach { prefs.remove(it) }
        }
    }

    private fun channelsKey(sourceUrl: String) = stringPreferencesKey("channels_$sourceUrl")
    private fun cacheTimeKey(sourceUrl: String) = longPreferencesKey("cache_time_$sourceUrl")

    @Serializable
    data class SerializableChannel(
        val id: String,
        val name: String,
        val url: String,
        val logoUrl: String? = null,
        val categoryName: String? = null,
    )

    private fun IptvChannel.toSerializable() = SerializableChannel(
        id = id, name = name, url = url, logoUrl = logoUrl, categoryName = categoryName
    )

    private fun SerializableChannel.toDomain() = IptvChannel(
        id = id, name = name, url = url, logoUrl = logoUrl, categoryName = categoryName
    )
}
