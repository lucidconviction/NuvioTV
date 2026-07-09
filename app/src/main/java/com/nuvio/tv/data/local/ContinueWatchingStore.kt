package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.continueWatchingDataStore: DataStore<Preferences> by preferencesDataStore(name = "continue_watching")

@Serializable
data class ContinueWatchingItem(
    val hubName: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val url: String,
    val timestamp: Long,
)

@Singleton
class ContinueWatchingStore @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("items")

    val itemsFlow: Flow<List<ContinueWatchingItem>> = context.continueWatchingDataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map emptyList()
        try { json.decodeFromString<List<ContinueWatchingItem>>(raw).sortedByDescending { it.timestamp }.take(10) }
        catch (_: Exception) { emptyList() }
    }

    suspend fun addItem(item: ContinueWatchingItem) {
        context.continueWatchingDataStore.edit { prefs ->
            val raw = prefs[key] ?: "[]"
            val current = try { json.decodeFromString<MutableList<ContinueWatchingItem>>(raw) } catch (_: Exception) { mutableListOf() }
            current.removeAll { it.url == item.url }
            current.add(0, item)
            prefs[key] = json.encodeToString(current.take(10))
        }
    }

    suspend fun removeItem(url: String) {
        context.continueWatchingDataStore.edit { prefs ->
            val raw = prefs[key] ?: return@edit
            val current = try { json.decodeFromString<MutableList<ContinueWatchingItem>>(raw) } catch (_: Exception) { return@edit }
            current.removeAll { it.url == url }
            prefs[key] = json.encodeToString(current)
        }
    }
}
