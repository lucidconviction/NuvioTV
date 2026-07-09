package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nuvio.tv.domain.model.IptvChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.channelHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "channel_history")

@Singleton
class ChannelHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val historyKey = stringPreferencesKey("iptv_channel_history")

    val historyFlow: Flow<List<IptvChannel>> = context.channelHistoryDataStore.data
        .map { prefs ->
            val raw = prefs[historyKey] ?: return@map emptyList()
            try {
                json.decodeFromString<List<SerializableChannel>>(raw).map { it.toDomain() }
            } catch (_: Exception) { emptyList() }
        }

    suspend fun addChannelToHistory(channel: IptvChannel) {
        context.channelHistoryDataStore.edit { prefs ->
            val raw = prefs[historyKey] ?: "[]"
            val current = try {
                json.decodeFromString<List<SerializableChannel>>(raw)
            } catch (_: Exception) { emptyList<SerializableChannel>() }

            val serialized = channel.toSerializable()
            val updated = (listOf(serialized) + current.filter { it.id != serialized.id }).take(20)
            prefs[historyKey] = json.encodeToString(updated)
        }
    }

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
