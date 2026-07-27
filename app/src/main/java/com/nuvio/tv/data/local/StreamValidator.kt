package com.robbdeeze.nuviotv.data.local

import android.util.Log
import com.robbdeeze.nuviotv.core.network.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.concurrent.TimeUnit

object StreamValidator {

    private const val TAG = "StreamValidator"
    private const val TIMEOUT_MS = 5_000
    private const val BATCH_SIZE = 20

    private val probeClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun validateUrls(
        urls: List<String>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Set<String> = withContext(Dispatchers.IO) {
        val dead = mutableSetOf<String>()
        val lock = Any()
        var completed = 0
        val total = urls.size
        coroutineScope {
            urls.chunked(BATCH_SIZE).forEach { batch ->
                batch.map { url ->
                    async {
                        if (!isUrlReachable(url)) {
                            synchronized(dead) { dead.add(url) }
                        }
                        synchronized(lock) {
                            completed++
                            onProgress(completed, total)
                        }
                    }
                }.awaitAll()
            }
        }
        dead
    }

    private fun isUrlReachable(url: String): Boolean {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Range", "bytes=0-0")
                .get()
                .build()
            probeClient.newCall(request).execute().use { response ->
                response.code == 200 || response.code == 206
            }
        }.getOrDefault(false)
    }
}
