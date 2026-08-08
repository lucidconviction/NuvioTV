package com.robbdeeze.nuviotv.data.sports.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class IcsParserTest {

    private val ufcFeed = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//UFC Calendar//Robbdeeze//EN
        BEGIN:VEVENT
        UID:ufc-323
        DTSTART:20260214T020000Z
        DTEND:20260214T070000Z
        SUMMARY:UFC 323: Chandler vs McGregor
        LOCATION:T-Mobile Arena\, Las Vegas
        URL:https://www.ufc.com/event/ufc-323
        DESCRIPTION:Pay-per-view event\\nMain event 2am EST
        END:VEVENT
        END:VCALENDAR
    """.trimIndent()

    @Test
    fun `parses single utc event with fields`() {
        val feed = IcsParser.parse(ufcFeed, source = "ufc-cal", league = "UFC")
        assertEquals(1, feed.events.size)
        val ev = feed.events.first()
        assertEquals("ufc-323", ev.uid)
        assertEquals("UFC 323: Chandler vs McGregor", ev.title)
        assertEquals("T-Mobile Arena, Las Vegas", ev.location)
        assertEquals("https://www.ufc.com/event/ufc-323", ev.url)
        assertEquals(LocalDateTime.parse("2026-02-14T02:00").atZone(ZoneOffset.UTC).toInstant().toEpochMilli(), ev.startEpochMillis)
        assertEquals(LocalDateTime.parse("2026-02-14T07:00").atZone(ZoneOffset.UTC).toInstant().toEpochMilli(), ev.endEpochMillis)
        assertEquals(false, ev.allDay)
        assertEquals("ufc-cal", ev.source)
        assertEquals("UFC", ev.league)
    }

    @Test
    fun `parses tzid datetime`() {
        val feed = IcsParser.parse("""
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:t1
            DTSTART;TZID=America/New_York:20260214T190000
            DTEND;TZID=America/New_York:20260214T220000
            SUMMARY:NFL: Chiefs @ Bills
            END:VEVENT
            END:VCALENDAR
        """.trimIndent(), source = "fixtur.es", league = "NFL")
        val ev = feed.events.first()
        assertEquals("NFL: Chiefs @ Bills", ev.title)
        val ny = ZoneId.of("America/New_York")
        val expectedStart = java.time.LocalDateTime.parse("2026-02-14T19:00")
            .atZone(ny).toInstant().toEpochMilli()
        assertEquals(expectedStart, ev.startEpochMillis)
    }

    @Test
    fun `expands weekly recurring events with byday`() {
        val feed = IcsParser.parse(
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:weekly-match
            DTSTART;VALUE=DATE:20260216
            RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE;COUNT=4
            SUMMARY:Soccer Club Session
            END:VEVENT
            END:VCALENDAR
            """.trimIndent(),
        )
        // COUNT=4 total occurrences: base (Mon 2/16) + 3 recurrences (Wed, Mon, Wed)
        assertEquals(4, feed.events.size)
        val recurrent = feed.events.filter { it.recurrent }
        assertEquals(3, recurrent.size)
        val dates = feed.events.map { it.startEpochMillis }.sorted()
        assertEquals(4, dates.distinct().size)
    }

    @Test
    fun `unescapes text`() {
        val feed = IcsParser.parse("""
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:e1
            DTSTART:20260101T000000Z
            SUMMARY:Fight\, fest tour
            DESCRIPTION:Line one\nLine two
            END:VEVENT
            END:VCALENDAR
        """.trimIndent())
        val ev = feed.events.first()
        assertEquals("Fight, fest tour", ev.title)
        assertEquals("Line one\nLine two", ev.description)
    }

    @Test
    fun `handles line folding`() {
        val ics = "BEGIN:VCALENDAR\n" +
            "BEGIN:VEVENT\n" +
            "UID:folded\n" +
            "DTSTART:20260101T120000Z\n" +
            "SUMMARY:This is a very lo\n" +
            " ng event title that continues on the n\n" +
            " ext line\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR"
        val feed = IcsParser.parse(ics)
        val ev = feed.events.first()
        assertEquals("This is a very long event title that continues on the next line", ev.title)
    }

    @Test
    fun `detects calendar timezone`() {
        val feed = IcsParser.parse("""
            BEGIN:VCALENDAR
            X-WR-TIMEZONE:Europe/London
            BEGIN:VEVENT
            UID:floating
            DTSTART:20260214T200000
            SUMMARY:EPL Match
            END:VEVENT
            END:VCALENDAR
        """.trimIndent())
        val ev = feed.events.first()
        val london = ZoneId.of("Europe/London")
        val expected = java.time.LocalDateTime.parse("2026-02-14T20:00")
            .atZone(london).toInstant().toEpochMilli()
        assertEquals(expected, ev.startEpochMillis)
    }

    @Test
    fun `parses all day date only event with one day duration`() {
        val feed = IcsParser.parse("""
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:allday
            DTSTART;VALUE=DATE:20260301
            SUMMARY:NBA All-Star Weekend
            END:VEVENT
            END:VCALENDAR
        """.trimIndent())
        val ev = feed.events.first()
        assertTrue(ev.allDay)
        assertEquals(
            LocalDate.parse("2026-03-01").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ev.startEpochMillis,
        )
        assertEquals(
            LocalDate.parse("2026-03-02").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ev.endEpochMillis,
        )
    }
}