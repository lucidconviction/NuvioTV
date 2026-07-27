package com.robbdeeze.nuviotv.data.local

import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamValidationStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
) {
    companion object {
        private const val TAG = "StreamValidation"
        private const val FEATURE = "stream_validation"
    }

    private val gson = Gson()

    private fun store(profileId: Int) = factory.get(profileId, FEATURE)

    private val deadKey = stringPreferencesKey("dead_urls")
    private val goodKey = stringPreferencesKey("good_urls")

    suspend fun getDeadUrls(profileId: Int): Set<String> {
        val prefs = store(profileId).data.first()
        val raw = prefs[deadKey] ?: return emptySet()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type).toSet()
        } catch (_: Exception) { emptySet() }
    }

    suspend fun markDead(urls: Set<String>, profileId: Int) {
        val current = getDeadUrls(profileId)
        val updated = current + urls
        store(profileId).edit { prefs ->
            prefs[deadKey] = gson.toJson(updated.toList())
        }
        Log.d(TAG, "Marked ${urls.size} streams as dead (total: ${updated.size})")
    }

    suspend fun markAlive(url: String, profileId: Int) {
        val current = getDeadUrls(profileId)
        if (url !in current) return
        store(profileId).edit { prefs ->
            prefs[deadKey] = gson.toJson((current - url).toList())
        }
    }

    suspend fun clearAll(profileId: Int) {
        store(profileId).edit { prefs ->
            prefs.remove(deadKey)
            prefs.remove(goodKey)
        }
    }

    suspend fun getGoodUrls(profileId: Int): Set<String> {
        val prefs = store(profileId).data.first()
        val raw = prefs[goodKey] ?: return emptySet()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type).toSet()
        } catch (_: Exception) { emptySet() }
    }

    suspend fun markGood(urls: Set<String>, profileId: Int) {
        val current = getGoodUrls(profileId)
        val updated = current + urls
        store(profileId).edit { prefs ->
            prefs[goodKey] = gson.toJson(updated.toList())
        }
    }
}
