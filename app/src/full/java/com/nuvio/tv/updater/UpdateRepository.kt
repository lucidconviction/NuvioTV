package com.robbdeeze.nuviotv.updater

import com.robbdeeze.nuviotv.updater.model.AppUpdate
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() {

    private companion object {
        const val APK_URL = "https://apps.rdnutz.us/"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getLatestUpdate(): Result<AppUpdate> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(APK_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) error("Server returned ${response.code}")
            val html = response.body!!.string()
            val apkRegex = Regex("""href="(RNutz-NuvioTV[^"]*\.apk)"""", RegexOption.IGNORE_CASE)
            val match = apkRegex.find(html)
            val apkFileName = match?.groupValues?.getOrNull(1) ?: error("No TV APK found on server")
            val apkUrl = APK_URL.trimEnd('/') + "/" + apkFileName

            AppUpdate(
                tag = "latest",
                title = "Nuvio TV Update",
                notes = "Download the latest APK from apps.rdnutz.us\n\n$apkFileName",
                releaseUrl = apkUrl,
                assetName = apkFileName,
                assetUrl = apkUrl,
                assetSizeBytes = 0L
            )
        }
    }
}
