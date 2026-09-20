package com.robbdeeze.nuviotv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.portalLicenseDataStore: DataStore<Preferences> by preferencesDataStore(name = "portal_license")

@Singleton
class PortalLicenseStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.portalLicenseDataStore

    private val licenseKey = stringPreferencesKey("license")
    private val fingerprintKey = stringPreferencesKey("fingerprint")
    private val installedPortalsKey = stringPreferencesKey("installed_portals")

    suspend fun loadLicense(): String? = dataStore.data.first()[licenseKey]
    suspend fun saveLicense(data: String) = dataStore.edit { it[licenseKey] = data }
    suspend fun clearLicense() = dataStore.edit { it.remove(licenseKey) }

    suspend fun loadFingerprint(): String? = dataStore.data.first()[fingerprintKey]
    suspend fun saveFingerprint(data: String) = dataStore.edit { it[fingerprintKey] = data }

    suspend fun loadInstalledPortals(): String? = dataStore.data.first()[installedPortalsKey]
    suspend fun saveInstalledPortals(data: String) = dataStore.edit { it[installedPortalsKey] = data }

    // Synchronous wrappers for use outside coroutine contexts (e.g. Compose composition)
    fun loadLicenseSync(): String? = runBlocking { loadLicense() }
    fun saveLicenseSync(data: String) = runBlocking { saveLicense(data) }
    fun loadFingerprintSync(): String? = runBlocking { loadFingerprint() }
    fun saveFingerprintSync(data: String) = runBlocking { saveFingerprint(data) }
}