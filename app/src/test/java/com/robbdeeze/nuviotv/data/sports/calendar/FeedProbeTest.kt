package com.robbdeeze.nuviotv.data.sports.calendar

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

class LiveFeedProbeTest {

    @Test
    fun probeFeeds() = runBlocking {
        val now = Instant.now()
        println("NOW epoch = ${now.toEpochMilli()}  ($now)")
        for ((leagueId, feeds) in SportCalendarFeedRegistry.byLeague) {
            for (feed in feeds) {
                try {
                    val parsed = CalendarFeedClient().fetchIcs(feed.url, source = "fixtur.es", league = feed.league)
                    val total = parsed.events.size
                    val next24 = parsed.events.count {
                        it.startEpochMillis in now.toEpochMilli()..now.plusSeconds(24 * 3600L).toEpochMilli()
                    }
                    val next60d = parsed.events.count {
                        it.startEpochMillis in now.toEpochMilli()..now.plusSeconds(60 * 24 * 3600L).toEpochMilli()
                    }
                    val recent = parsed.events.count { it.startEpochMillis <= now.toEpochMilli() }
                    println("$leagueId | ${feed.url.take(48)} | total=$total next24h=$next24 next60d=$next60d past=$recent")
                } catch (e: Exception) {
                    println("$leagueId | ERROR: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }

        // Repo-level probes
        for (leagueId in listOf("epl", "nfl", "nba", "ufc", "wnba")) {
            try {
                val u = SportsCalendarRepository.upcomingFor(leagueId, leagueId.uppercase())
                println("REPO upcomingFor($leagueId) -> ${u.size} events")
            } catch (e: Exception) {
                println("REPO upcomingFor($leagueId) ERROR: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        try {
            val all = SportsCalendarRepository.allUpcoming()
            println("REPO allUpcoming() -> ${all.size} events")
        } catch (e: Exception) {
            println("REPO allUpcoming ERROR: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}