package com.robbdeeze.nuviotv.data.local

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.robbdeeze.nuviotv.core.profile.ProfileManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AddonPreferences @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        private const val FEATURE = "addon_preferences"
    }

    private fun effectiveProfileId(): Int {
        val active = profileManager.activeProfile
        return if (active != null && active.usesPrimaryAddons) 1 else profileManager.activeProfileId.value
    }

    private fun store(profileId: Int = effectiveProfileId()) =
        factory.get(profileId, FEATURE)

    private val effectiveProfileIdFlow: Flow<Int> = combine(
        profileManager.activeProfileId,
        profileManager.profiles
    ) { activeProfileId, profiles ->
        val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
        if (activeProfile?.usesPrimaryAddons == true) 1 else activeProfileId
    }.distinctUntilChanged()

    private val gson = Gson()
    private val orderedUrlsKey = stringPreferencesKey("installed_addon_urls_ordered")
    private val legacyUrlsKey = stringSetPreferencesKey("installed_addon_urls")
    private val userSetNamesKey = stringPreferencesKey("addon_user_set_names")
    private val addonEnabledStatesKey = stringPreferencesKey("installed_addon_enabled_states")
    private val manifestSuffix = "/manifest.json"

    private fun canonicalizeUrl(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        val queryStart = trimmed.indexOf('?')
        val path = if (queryStart >= 0) trimmed.substring(0, queryStart) else trimmed
        val query = if (queryStart >= 0) trimmed.substring(queryStart) else ""
        val cleanPath = if (path.endsWith(manifestSuffix, ignoreCase = true)) {
            path.dropLast(manifestSuffix.length).trimEnd('/')
        } else {
            path.trimEnd('/')
        }
        return cleanPath + query
    }

    val installedAddonUrls: Flow<List<String>> = effectiveProfileIdFlow.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { preferences ->
            val json = preferences[orderedUrlsKey]
            if (json != null) {
                parseUrlList(json)
            } else {
                val legacySet = preferences[legacyUrlsKey] ?: getDefaultAddons()
                legacySet.toList()
            }
        }
    }

    val addonEnabledStates: Flow<Map<String, Boolean>> = effectiveProfileIdFlow.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { preferences ->
            preferences[addonEnabledStatesKey]
                ?.let(::parseEnabledStateMap)
                .orEmpty()
        }
    }

    suspend fun ensureMigrated() {
        val ds = store()
        val prefs = ds.data.first()
        if (prefs[orderedUrlsKey] == null) {
            val legacySet = prefs[legacyUrlsKey] ?: getDefaultAddons()
            ds.edit { preferences ->
                preferences[orderedUrlsKey] = gson.toJson(legacySet.toList())
                preferences.remove(legacyUrlsKey)
            }
        }
    }

    suspend fun addAddon(url: String) {
           val active = profileManager.activeProfile
           if (active != null && !active.isPrimary && active.usesPrimaryAddons) return
        store().edit { preferences ->
            val current = getCurrentList(preferences)
            val normalizedUrl = canonicalizeUrl(url)
            if (current.any { canonicalizeUrl(it).equals(normalizedUrl, ignoreCase = true) }) return@edit
            preferences[orderedUrlsKey] = gson.toJson(current + normalizedUrl)
            val states = getCurrentEnabledStates(preferences).toMutableMap()
            states[normalizedUrl] = true
            preferences[addonEnabledStatesKey] = gson.toJson(states)
        }
    }

    suspend fun removeAddon(url: String) {
           val active = profileManager.activeProfile
           if (active != null && !active.isPrimary && active.usesPrimaryAddons) return
        store().edit { preferences ->
            val current = getCurrentList(preferences).toMutableList()
            val normalizedUrl = canonicalizeUrl(url)

            val indexToRemove = current.indexOfFirst {
                canonicalizeUrl(it).equals(normalizedUrl, ignoreCase = true)
            }
            if (indexToRemove != -1) {
                current.removeAt(indexToRemove)
            }
            preferences[orderedUrlsKey] = gson.toJson(current)
            val states = getCurrentEnabledStates(preferences).toMutableMap()
            states.remove(normalizedUrl)
            preferences[addonEnabledStatesKey] = gson.toJson(states)
        }
    }

    suspend fun setAddonOrder(urls: List<String>) {
            val active = profileManager.activeProfile
            if (active != null && !active.isPrimary && active.usesPrimaryAddons) return
        store().edit { preferences ->
            val orderedUrls = urls.map(::canonicalizeUrl)
            preferences[orderedUrlsKey] = gson.toJson(orderedUrls)
            val currentStates = getCurrentEnabledStates(preferences)
            preferences[addonEnabledStatesKey] = gson.toJson(
                orderedUrls.associateWith { url -> currentStates[url] ?: true }
            )
        }
    }

    suspend fun setAddonEnabled(url: String, enabled: Boolean) {
        val active = profileManager.activeProfile
        if (active != null && !active.isPrimary && active.usesPrimaryAddons) return
        store().edit { preferences ->
            val states = getCurrentEnabledStates(preferences).toMutableMap()
            states[canonicalizeUrl(url)] = enabled
            preferences[addonEnabledStatesKey] = gson.toJson(states)
        }
    }

    suspend fun setAddonEnabledStates(states: Map<String, Boolean>) {
        val active = profileManager.activeProfile
        if (active != null && !active.isPrimary && active.usesPrimaryAddons) return
        store().edit { preferences ->
            preferences[addonEnabledStatesKey] = gson.toJson(
                states.mapKeys { (url, _) -> canonicalizeUrl(url) }
            )
        }
    }

    private fun getCurrentList(preferences: Preferences): List<String> {
        val json = preferences[orderedUrlsKey]
        return if (json != null) {
            parseUrlList(json)
        } else {
            val legacySet = preferences[legacyUrlsKey] ?: getDefaultAddons()
            legacySet.toList()
        }
    }

    private fun parseUrlList(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: getDefaultAddons().toList()
        } catch (e: Exception) {
            getDefaultAddons().toList()
        }
    }

    val userSetNames: Flow<Map<String, String>> = effectiveProfileIdFlow.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { preferences ->
            val json = preferences[userSetNamesKey]
            if (json != null) parseNameMap(json) else emptyMap()
        }
    }

    suspend fun setUserSetNames(names: Map<String, String>) {
        store().edit { preferences ->
            preferences[userSetNamesKey] = gson.toJson(
                names.mapKeys { (url, _) -> canonicalizeUrl(url) }
            )
        }
    }

    private fun parseNameMap(json: String): Map<String, String> {
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val parsed: Map<String, String> = gson.fromJson(json, type) ?: emptyMap()
            parsed.mapKeys { (url, _) -> canonicalizeUrl(url) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun getCurrentEnabledStates(preferences: Preferences): Map<String, Boolean> {
        val json = preferences[addonEnabledStatesKey] ?: return emptyMap()
        return parseEnabledStateMap(json)
    }

    private fun parseEnabledStateMap(json: String): Map<String, Boolean> {
        return try {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            val parsed: Map<String, Boolean> = gson.fromJson(json, type) ?: emptyMap()
            parsed.mapKeys { (url, _) -> canonicalizeUrl(url) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun getDefaultAddons(): Set<String> = setOf(
        "https://v3-cinemeta.strem.io",
        "https://opensubtitles-v3.strem.io",
        "https://hdhub.thevolecitor.qzz.io/eyJ0b3Jib3giOiJ1bnNldCIsInF1YWxpdGllcyI6IjIxNjBwLDEwODBwLDcyMHAsNDgwcCIsInNvcnQiOiJkZXNjIiwiY2F0YWxvZ3MiOiJmZWF0dXJlZCxwb3B1bGFyIn0",
        "https://mediafusion.elfhosted.com/D-NpFBMMJFCws06gObWz2gq4CA_duUjy6nUK-VbjPi2uWv6pAtUpzLwS7t89VaoMqtOz792MkGEeSTweVfY6msu4p89PZmISOXOuL0V4VKOiICzA-nj13qFBtLKoB9q8jzMi-RD9iZNCOVMA81ILXK2HeRyteevl1eS3pib6fKep_cdYJ-waojnaSn9ApTBVbADMCGyn9sc1WotOLDlAUZfHxsUyTyfaRA_9Tz8VbyRtBHDhint0iOg4HAl5dARz9P3Jy-vly3skUz2ZEI0vFaNMtlCYPddppC2FDIs_3786noXT9_EzJYj_0z-HcQmLJI-ZIYnzaUdKwLQ_iFlMX3BoC1SrmM-Fvshy2vl8QlRMs6jhnSMRLh9XOsH9tWXi8UNtClkwBkqDrFxdZsh53z3uYcd7_mmwMrpCtjha-q8f8cBL95JI9WCDp4mJQcscHJWWopz_-f4EDCEy-4o6X9V55jUzyUkC2ykbDeIq0A17oz1-seNhMNFDihEqhnO6mbHYsbiZW9nulTvN1ezdKidsmdao1wBZ9j5P5gIj6tzf2Pqm6Q6zvuKRr8aHCTsIQnrPE4fKAx6eUXZuxZlWYNzYcurgOGUIDHt__g1S6_egkiJ8EKvCNGeGm2Wn29XHaTAIKD2szucWWjZTM9SoS74PJvnPWcZ1ow7uZrSJ6n-lqBh-AOemZEIkOY4gxe98jaQIw-CCyfZmbpuAX6OLm5YyrAskDfYiIQJWAr2b7d1z6dgr9h96VA22zfIfcwAMPTHf6nXnClgRdBCvD-SXb7HEVD346ic6ESxihD0llXGljkpp5yAdebSJFR6YPIU5jZbQGQoJMAZLjKXzh-vsJFLUGUgI6Cnna79b0wGd6iAN1eT6m6SGZoim7tt_wsFChZNIHCKgf0mLSBCuz_9gHRv9CF_PvtcT6t0GxpATcyzS_CWFvH6n0nuGEyGROa7HZoT3qxJ6CbMWZorQAl9tEYtwH1NX7xZk2ecnC3gvHEjqp1_qh2uua6ERpvJMDPXVW1F-Tw4TMsxDl69UmTM0dtWA",
        "https://87d6a6ef6b58-webstreamrmbg.baby-beamup.club/%7B%22multi%22%3A%22on%22%7D",
        "https://torrentio.strem.fun",
        "https://live-sport-plugin-g0l1.onrender.com",
        "https://torrentsdb.com",
        "https://pengu.uk",
        "https://ytztvio.galacticcapsule.workers.dev",
        "https://nodebrid.fly.dev"
    )
}
