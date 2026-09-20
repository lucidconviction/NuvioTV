package com.robbdeeze.nuviotv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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

    suspend fun loadLicense(): String? = withContext(Dispatchers.IO) {
        dataStore.data.first()[licenseKey]
    }

    suspend fun saveLicense(data: String): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { it[licenseKey] = data }
    }

    suspend fun clearLicense(): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { it.remove(licenseKey) }
    }

    suspend fun loadFingerprint(): String? = withContext(Dispatchers.IO) {
        dataStore.data.first()[fingerprintKey]
    }

    suspend fun saveFingerprint(data: String): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { it[fingerprintKey] = data }
    }

    suspend fun loadInstalledPortals(): String? = withContext(Dispatchers.IO) {
        dataStore.data.first()[installedPortalsKey]
    }

    suspend fun saveInstalledPortals(data: String): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { it[installedPortalsKey] = data }
    }
}