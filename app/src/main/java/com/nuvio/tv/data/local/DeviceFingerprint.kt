package com.robbdeeze.nuviotv.data.local

import android.os.Build
import android.provider.Settings
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class DeviceFingerprint @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getDeviceId(): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) { null }

        val fingerprint = listOf(
            androidId,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.BRAND,
            Build.DEVICE,
            Build.HARDWARE,
        ).joinToString("|")

        return fingerprint.ifBlank { UUID.randomUUID().toString() }
    }
}