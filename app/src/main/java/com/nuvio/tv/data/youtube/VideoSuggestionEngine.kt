package com.robbdeeze.nuviotv.data.youtube

import com.robbdeeze.nuviotv.domain.model.VidNutzVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object VideoSuggestionEngine {

    private val seenIds = ConcurrentHashMap<String, MutableSet<String>>()
    private val rotationCounters = ConcurrentHashMap<String, Int>()
    private val affixes = listOf("", " amazing", " incredible", " crazy", " best", " top", " 2026", " this week")

    private val queryRotations = mapOf(
        "Trending" to listOf("trending videos", "viral videos today", "popular now", "trending 2026"),
        "News" to listOf("latest news today", "breaking news", "top stories", "news headlines"),
        "Music" to listOf("music videos", "new music 2026", "top songs", "viral music"),
        "Sports" to listOf("sports highlights", "sports moments", "sports 2026", "epic sports"),
        "Politics" to listOf("politics today", "political news", "politics 2026", "government news"),
        "Documentary" to listOf("documentary 2026", "full documentary", "documentary films", "nature documentary"),
        "Technology" to listOf("tech news", "technology 2026", "gadgets review", "future tech"),
        "Entertainment" to listOf("entertainment news", "celebrity news", "tv shows", "entertainment today"),
        "Comedy" to listOf("funny videos", "comedy stand up", "funny moments", "hilarious"),
        "Science" to listOf("science news", "space 2026", "scientific discovery", "physics"),
        "True Crime" to listOf("true crime documentary", "true crime stories", "crime mystery", "cold case"),
        "Food & Drink" to listOf("cooking recipes", "food 2026", "delicious food", "street food"),
    )

    suspend fun suggest(
        query: String,
        categoryKey: String = "Trending",
        count: Int = 32,
    ): List<VidNutzVideo> = withContext(Dispatchers.IO) {
        val rotations = queryRotations[categoryKey] ?: queryRotations["Trending"]!!
        val rotationIdx = rotationCounters.getOrPut(categoryKey) { 0 }
        rotationCounters[categoryKey] = (rotationIdx + 1) % rotations.size
        val baseQuery = rotations[rotationIdx]
        val affix = affixes[rotationIdx % affixes.size]
        val fullQuery = "$baseQuery$affix"
        val results = PlatformYouTubeSearch.search(fullQuery)
        val seen = seenIds.getOrPut(categoryKey) { mutableSetOf() }
        val fresh = results.filter { it.videoId !in seen }
        val finalList = fresh.take(count)
        if (finalList.size < count && results.isNotEmpty()) {
            seen.clear()
            val retry = results.filter { it.videoId !in seen }.take(count)
            finalList.toSet().forEach { seen.add(it.videoId) }
            return@withContext retry
        }
        finalList.forEach { seen.add(it.videoId) }
        return@withContext finalList
    }

    fun resetCategory(categoryKey: String) {
        seenIds.remove(categoryKey)
        rotationCounters.remove(categoryKey)
    }
}
