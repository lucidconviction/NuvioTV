package com.robbdeeze.nuviotv.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.robbdeeze.nuviotv.domain.model.QuickChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.customQuickChannelDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "custom_quick_channels"
)

private val CUSTOM_QC_KEY = stringSetPreferencesKey("custom_quick_channels")

@Singleton
class QuickChannelPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val customQuickChannels: Flow<List<QuickChannel>> = context.customQuickChannelDataStore.data.map { prefs ->
        prefs[CUSTOM_QC_KEY]?.mapNotNull { entry ->
            val parts = entry.split("\u0001")
            if (parts.size >= 4) QuickChannel(
                displayName = parts[0],
                aliases = parts[1].split("\u0002").filter { it.isNotBlank() },
                regions = parts[2].split("\u0002").filter { it.isNotBlank() },
                tags = parts[3].split("\u0002").filter { it.isNotBlank() },
            ) else null
        } ?: emptyList()
    }

    suspend fun addCustomQuickChannel(channel: QuickChannel) {
        context.customQuickChannelDataStore.edit { prefs ->
            val existing = prefs[CUSTOM_QC_KEY]?.toMutableSet() ?: mutableSetOf()
            val entry = "${channel.displayName}\u0001${channel.aliases.joinToString("\u0002")}\u0001${channel.regions.joinToString("\u0002")}\u0001${channel.tags.joinToString("\u0002")}"
            existing.add(entry)
            prefs[CUSTOM_QC_KEY] = existing
        }
    }

    suspend fun removeCustomQuickChannel(channel: QuickChannel) {
        context.customQuickChannelDataStore.edit { prefs ->
            val existing = prefs[CUSTOM_QC_KEY]?.toMutableSet() ?: mutableSetOf()
            val entry = "${channel.displayName}\u0001${channel.aliases.joinToString("\u0002")}\u0001${channel.regions.joinToString("\u0002")}\u0001${channel.tags.joinToString("\u0002")}"
            existing.remove(entry)
            prefs[CUSTOM_QC_KEY] = existing
        }
    }

    suspend fun clearCustomQuickChannels() {
        context.customQuickChannelDataStore.edit { prefs ->
            prefs.remove(CUSTOM_QC_KEY)
        }
    }
}