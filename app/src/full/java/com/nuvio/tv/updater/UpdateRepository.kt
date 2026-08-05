package com.robbdeeze.nuviotv.updater

import com.robbdeeze.nuviotv.data.remote.dto.GitHubReleaseDto
import com.robbdeeze.nuviotv.updater.model.AppUpdate
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() {

    private companion object {
        const val GITHUB_API_URL = "https://api.github.com/repos/lucidconviction/NuvioTV/releases/latest"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()

    suspend fun getLatestUpdate(): Result<AppUpdate> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) error("GitHub API returned ${response.code}")
            val body = response.body!!.string()

            val adapter = moshi.adapter(GitHubReleaseDto::class.java)
            val release = adapter.fromJson(body) ?: error("Failed to parse GitHub release")

            val tag = release.tagName ?: error("Release has no tag")
            val title = release.name ?: "Nuvio TV $tag"
            val notes = release.body ?: ""
            val releaseUrl = release.htmlUrl
            val assets = release.assets

            val bestAsset = AbiSelector.chooseBestApkAsset(assets)
                ?: error("No APK assets found in latest release")

            AppUpdate(
                tag = tag,
                title = title,
                notes = notes,
                releaseUrl = releaseUrl,
                assetName = bestAsset.name,
                assetUrl = bestAsset.browserDownloadUrl,
                assetSizeBytes = bestAsset.size
            )
        }
    }
}