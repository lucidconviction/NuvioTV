package com.robbdeeze.nuviotv.data.sports.calendar

import java.io.InputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Minimal, dependency-free RFC-5545 (iCalendar) parser tailored for sports calendar feeds.
 * Supports VCALENDAR/VEVENT, line unfolding, property parameters, UTC / TZID / floating
 * timestamp forms, DURATION, X-WR-TIMEZONE, and RRULE recurrence (daily / weekly with BYDAY).
 */
object IcsParser {

    private const val HORIZON_DAYS = 62L

    fun parse(data: String, source: String = "", league: String = ""): CalendarFeed {
        val calTz = detectCalendarTimeZone(data)
        val events = mutableListOf<CalendarEvent>()
        var currentComponent: String? = null
        val block = mutableMapOf<String, MutableList<RawProp>>()

        fun flushComponent() {
            if (currentComponent == "VEVENT" && block.isNotEmpty()) {
                parseVEvent(block, calTz, source, league)?.let { events.addAll(it) }
            }
            block.clear()
        }

        for (line in unfold(data)) {
            when {
                line.startsWith("BEGIN:") -> { flushComponent(); currentComponent = line.substring(6) }
                line.startsWith("END:") -> { flushComponent(); currentComponent = null }
                else -> {
                    val sep = line.indexOf(':')
                    if (sep <= 0) continue
                    val (name, params) = parsePropertyKey(line.substring(0, sep))
                    val value = line.substring(sep + 1)
                    block.getOrPut(name) { mutableListOf() }.add(RawProp(value, params))
                }
            }
        }
        flushComponent()
        return CalendarFeed(name = "", source = source, events = events)
    }

    private data class RawProp(val value: String, val params: Map<String, String>)

    fun parse(stream: InputStream, source: String = "", league: String = ""): CalendarFeed {
        val data = stream.readBytes().toString(Charsets.UTF_8)
        return parse(data, source, league)
    }

    private fun detectCalendarTimeZone(data: String): ZoneId? {
        val raw = Regex("(?:^|\r?\n)X-WR-TIMEZONE[:;]\\s*([^\\r\\n]+)")
            .find(data)?.groupValues?.get(1)?.trim() ?: return null
        return try { ZoneId.of(raw) } catch (_: Exception) { null }
    }

    private fun parsePropertyKey(keyValue: String): Pair<String, Map<String, String>> {
        val semi = keyValue.indexOf(';')
        if (semi < 0) return keyValue to emptyMap()
        val name = keyValue.substring(0, semi)
        val params = mutableMapOf<String, String>()
        for (token in keyValue.substring(semi + 1).split(";")) {
            val eq = token.indexOf('=')
            if (eq <= 0) continue
            params[token.substring(0, eq).uppercase(Locale.US)] = token.substring(eq + 1).trim('"')
        }
        return name to params
    }

    private fun parseVEvent(
        props: Map<String, List<RawProp>>,
        calTz: ZoneId?,
        source: String,
        league: String,
    ): List<CalendarEvent>? {
        val summary = unescape(props["SUMMARY"]?.firstOrNull()?.value ?: return null)
        val uid = (props["UID"]?.firstOrNull()?.value ?: summary).trim()
        val dtStart = props["DTSTART"]?.firstOrNull() ?: return null
        val start = parseDateTime(dtStart, calTz) ?: return null
        val isAllDay = !dtStart.value.contains('T')

        val end: ZonedDateTime? = when {
            props["DTEND"]?.firstOrNull() != null -> parseDateTime(props["DTEND"]!!.first()!!, calTz)
            props["DURATION"]?.firstOrNull() != null ->
                start.plusSeconds(parseDuration(props["DURATION"]!!.first()!!.value))
            isAllDay -> start.plusDays(1)
            else -> null
        }

        val base = CalendarEvent(
            uid = uid,
            title = summary,
            startEpochMillis = start.toEpochSecond() * 1000L,
            endEpochMillis = end?.toEpochSecond()?.times(1000L),
            allDay = isAllDay,
            description = props["DESCRIPTION"]?.firstOrNull()?.value?.let { unescape(it) },
            location = props["LOCATION"]?.firstOrNull()?.value?.let { unescape(it) },
            url = props["URL"]?.firstOrNull()?.value,
            league = league,
            source = source,
        )

        val rrule = props["RRULE"]?.firstOrNull()?.value
        if (rrule == null) return listOf(base)

        val recurrence = expandRecurrence(base, rrule, start.toLocalDate(), end?.toLocalDate())
        return recurrence + base
    }

    private fun expandRecurrence(
        base: CalendarEvent,
        rrule: String,
        startDate: LocalDate,
        endDate: LocalDate?,
    ): List<CalendarEvent> {
        val freq = Regex("FREQ=([A-Z]+)").find(rrule)?.groupValues?.get(1)?.uppercase(Locale.US) ?: return emptyList()
        val interval = Regex("INTERVAL=(\\d+)").find(rrule)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val byDay = Regex("BYDAY=([A-Z]{2}(?:,[A-Z]{2})*)").find(rrule)?.groupValues?.get(1)
        val untilRaw = Regex("UNTIL=([^;]+)").find(rrule)?.groupValues?.get(1)
        val count = Regex("COUNT=(\\d+)").find(rrule)?.groupValues?.get(1)?.toIntOrNull()
        val until: LocalDate? = untilRaw?.let {
            if (it.contains('T')) LocalDate.parse(it.take(8), DateTimeFormatter.BASIC_ISO_DATE)
            else LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE)
        }

        val horizonEnd = LocalDate.now().plusDays(HORIZON_DAYS)
        val realEnd = until?.let { if (it.isBefore(horizonEnd)) it else horizonEnd } ?: horizonEnd
        val results = mutableListOf<CalendarEvent>()
        var counter = 1

        if (freq == "DAILY") {
            var d = startDate.plusDays(interval.toLong())
            while (!d.isAfter(realEnd)) {
                results.add(base.copy(
                    uid = "${base.uid}-R${counter++}",
                    startEpochMillis = startOfDayInMillis(base, d),
                    endEpochMillis = endDate?.let { startOfDayInMillis(base, it) },
                    recurrent = true,
                ))
                d = d.plusDays(interval.toLong())
                if (count != null && counter >= count) break
            }
        } else if (freq == "WEEKLY") {
            val days = byDay?.split(",")?.mapNotNull { parseDay(it) }
            var weekStart = startDate
            while (!weekStart.isAfter(realEnd)) {
                if (days == null) {
                    val d = weekStart
                    if (d.isAfter(startDate) && !d.isAfter(realEnd)) {
                        results.add(base.copy(
                            uid = "${base.uid}-R${counter++}",
                            startEpochMillis = startOfDayInMillis(base, d),
                            endEpochMillis = endDate?.let { startOfDayInMillis(base, it) },
                            recurrent = true,
                        ))
                    }
                } else {
                    for (day in days) {
                        val d = weekStart.with(TemporalAdjusters.nextOrSame(day))
                        if (!d.isAfter(startDate) || d.isAfter(realEnd)) continue
                        results.add(base.copy(
                            uid = "${base.uid}-R${counter++}",
                            startEpochMillis = startOfDayInMillis(base, d),
                            endEpochMillis = endDate?.let { startOfDayInMillis(base, it) },
                            recurrent = true,
                        ))
                        if (count != null && counter >= count) return results
                    }
                }
                weekStart = weekStart.plusWeeks(interval.toLong())
                if (count != null && counter >= count) break
            }
        }
        return results
    }

    private fun startOfDayInMillis(base: CalendarEvent, date: LocalDate): Long {
        val original = LocalDateTime.ofInstant(Instant.ofEpochMilli(base.startEpochMillis), ZoneId.systemDefault())
        val shifted = original.withYear(date.year).withMonth(date.monthValue).withDayOfMonth(date.dayOfMonth)
        return shifted.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L
    }

    private fun parseDay(s: String): DayOfWeek? = when (s.uppercase(Locale.US)) {
        "MO" -> DayOfWeek.MONDAY; "TU" -> DayOfWeek.TUESDAY; "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY; "FR" -> DayOfWeek.FRIDAY; "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY; else -> null
    }

    private fun parseDuration(raw: String): Long {
        val m = Regex("P(\\d+D)?T?(\\d+H)?(\\d+M)?(\\d+S)?").matchEntire(raw) ?: return 0L
        var ms = 0L
        ms += (m.groupValues[1].trimEnd('D').toLongOrNull() ?: 0L) * 86_400_000L
        ms += (m.groupValues[2].trimEnd('H').toLongOrNull() ?: 0L) * 3_600_000L
        ms += (m.groupValues[3].trimEnd('M').toLongOrNull() ?: 0L) * 60_000L
        ms += (m.groupValues[4].trimEnd('S').toLongOrNull() ?: 0L) * 1000L
        return ms / 1000L
    }

    private fun parseDateTime(prop: RawProp, calTz: ZoneId?): ZonedDateTime? {
        val value = prop.value
        val tzId = prop.params["TZID"]
        val isUtc = value.endsWith("Z", ignoreCase = true)
        val clean = value.trimEnd('Z')
        return try {
            when {
                isUtc -> LocalDateTime.parse(clean, BASIC_DATE_TIME).atZone(ZoneOffset.UTC)
                tzId != null -> {
                    val zone = try { ZoneId.of(tzId) } catch (_: Exception) { calTz ?: ZoneOffset.UTC }
                    LocalDateTime.parse(clean, BASIC_DATE_TIME).atZone(zone)
                }
                clean.length <= 8 -> {
                    val date = LocalDate.parse(clean, DateTimeFormatter.BASIC_ISO_DATE)
                    date.atStartOfDay(calTz ?: ZoneId.systemDefault())
                }
                else -> {
                    val local = LocalDateTime.parse(clean, BASIC_DATE_TIME)
                    local.atZone(calTz ?: ZoneId.systemDefault())
                }
            }
        } catch (_: Exception) { null }
    }

    private fun unescape(s: String): String = s
        .replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")

    private fun unfold(data: String): List<String> {
        val out = mutableListOf<String>()
        var current = StringBuilder()
        for (line in data.split("\r\n", "\n")) {
            val trimmed = line.trimEnd('\r')
            if (trimmed.startsWith(" ") || trimmed.startsWith("\t")) {
                current.append(trimmed.drop(1))
            } else {
                if (current.isNotEmpty()) out.add(current.toString())
                current = StringBuilder(trimmed)
            }
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out
    }

    private val BASIC_DATE_TIME = DateTimeFormatterBuilder()
        .appendPattern("yyyyMMdd'T'HHmmss")
        .toFormatter()
}