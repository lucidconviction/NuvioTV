package com.nuvio.tv.data.iptv

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.domain.model.IptvChannel
import com.nuvio.tv.domain.model.IptvSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvStorage @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        private const val FEATURE = "iptv_settings"
    }

    private val gson = Gson()
    private val favoritesKey = stringPreferencesKey("iptv_favorites")
    private val sourcesKey = stringPreferencesKey("iptv_sources")

    private fun store(profileId: Int = profileManager.activeProfileId.value) =
        factory.get(profileId, FEATURE)

    val favorites: Flow<List<IptvChannel>> = profileManager.activeProfileId.flatMapLatest { profileId ->
        factory.get(profileId, FEATURE).data.map { prefs ->
            val raw = prefs[favoritesKey]
            if (raw.isNullOrBlank()) emptyList()
            else {
                try {
                    val type = object : TypeToken<List<IptvChannel>>() {}.type
                    gson.fromJson(raw, type)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    val sources: Flow<List<IptvSource>> = profileManager.activeProfileId.flatMapLatest { profileId ->
        factory.get(profileId, FEATURE).data.map { prefs ->
            val raw = prefs[sourcesKey]
            if (raw.isNullOrBlank()) emptyList()
            else {
                try {
                    val type = object : TypeToken<List<IptvSource>>() {}.type
                    gson.fromJson(raw, type)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    suspend fun addFavorite(channel: IptvChannel) {
        val current = favorites.first()
        if (current.any { it.id == channel.id }) return
        val updated = current + channel
        store().edit { prefs ->
            prefs[favoritesKey] = gson.toJson(updated)
        }
    }

    suspend fun removeFavorite(channelId: String) {
        val current = favorites.first()
        val updated = current.filterNot { it.id == channelId }
        store().edit { prefs ->
            prefs[favoritesKey] = gson.toJson(updated)
        }
    }

    suspend fun addSource(source: IptvSource) {
        val current = sources.first()
        if (current.any { it.url == source.url }) return
        val updated = current + source
        store().edit { prefs ->
            prefs[sourcesKey] = gson.toJson(updated)
        }
    }

    suspend fun removeSource(url: String) {
        val current = sources.first()
        val updated = current.filterNot { it.url == url }
        store().edit { prefs ->
            prefs[sourcesKey] = gson.toJson(updated)
        }
    }
}
