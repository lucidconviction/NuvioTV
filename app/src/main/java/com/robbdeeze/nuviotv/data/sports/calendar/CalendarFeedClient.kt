package com.robbdeeze.nuviotv.data.sports.calendar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Generic iCal feed fetcher with in-memory caching (configurable TTL), per-host
 * request throttling, and user-agent rotation.
 */
class CalendarFeedClient(
    private val client: OkHttpClient = defaultClient(),
    private val cacheTtlMillis: Long = CACHE_TTL_MS,
) {
    private val cache = ConcurrentHashMap<String, CachedFeed>()
    private val hostThrottle = ConcurrentHashMap<String, Long>()
    private val throttleMutex = Mutex()

    private data class CachedFeed(
        val body: String,
        val fetchedAt: Long,
    )

    suspend fun fetchIcs(url: String, source: String = url, league: String = "", force: Boolean = false): CalendarFeed {
        val body = fetchRaw(url, force)
        return withContext(Dispatchers.Default) { IcsParser.parse(body, source, league) }
    }

    suspend fun fetchRaw(url: String, force: Boolean = false): String {
        val cached = cache[url]
        if (!force && cached != null && (System.currentTimeMillis() - cached.fetchedAt) < cacheTtlMillis) {
            return cached.body
        }
        val raw = fetch(url)
        cache[url] = CachedFeed(raw, System.currentTimeMillis())
        return raw
    }

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val host = runCatching { url.toHttpUrl().host }.getOrDefault(url)
        throttle(host)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgents.random())
            .header("Accept", "text/calendar, text/plain;q=0.9, */*;q=0.5")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} for $url")
            response.body?.string() ?: ""
        }
    }

    private suspend fun throttle(host: String) {
        val now = System.currentTimeMillis()
        val last = hostThrottle[host] ?: 0L
        val wait = THROTTLE_MIN_INTERVAL_MS - (now - last)
        if (wait > 0) {
            throttleMutex.withLock {
                kotlinx.coroutines.delay(wait)
            }
        }
        hostThrottle[host] = System.currentTimeMillis()
    }

    fun clearCache() {
        cache.clear()
    }

    fun invalidate(url: String) {
        cache.remove(url)
    }

    private companion object {
        const val CACHE_TTL_MS = 30 * 60 * 1000L
        const val THROTTLE_MIN_INTERVAL_MS = 400L
        val userAgents = listOf(
            "NuvioTV/1.0 (Android; SportNutz)",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36",
        )
    }
}

private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()