package com.robbdeeze.nuviotv.data.sports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DaddyLiveClientTest {

    @Test
    fun `parses live time marker`() {
        val ms = DaddyLiveEvent.parseEventTime("Saturday 8th August 2026 - Schedule Time UK GMT", "Live")
        assertTrue(ms > 0L)
    }

    @Test
    fun `parses weekday ordinal day format with uk times`() {
        val ms = DaddyLiveEvent.parseEventTime("Saturday 8th August 2026 - Schedule Time UK GMT", "13:00")
        assertTrue("parsed to $ms", ms > 0L)
    }

    @Test
    fun `parses 12h am pm`() {
        val pm = DaddyLiveEvent.parseEventTime("Saturday 8th August 2026 - Schedule Time UK GMT", "1:00 PM GMT")
        val am = DaddyLiveEvent.parseEventTime("Saturday 8th August 2026 - Schedule Time UK GMT", "1:00 AM")
        assertTrue(pm > am)
    }

    @Test
    fun `parses mm slash yyyy still works`() {
        val ms = DaddyLiveEvent.parseEventTime("08/08/2026", "19:00")
        assertTrue(ms > 0L)
        assertEquals(
            java.time.LocalDateTime.parse("2026-08-08T19:00")
                .atZone(java.time.ZoneId.of("Europe/London")).toInstant().toEpochMilli(),
            ms,
        )
    }
}
