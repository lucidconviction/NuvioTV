package com.robbdeeze.nuviotv.data.sports.calendar

data class CalendarEvent(
    val uid: String,
    val title: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long? = null,
    val allDay: Boolean = false,
    val description: String? = null,
    val location: String? = null,
    val url: String? = null,
    val sport: String = "",
    val league: String = "",
    val homeTeam: String? = null,
    val awayTeam: String? = null,
    val recurrent: Boolean = false,
    val source: String = "",
) {
    val isUpcoming: Boolean get() = startEpochMillis > System.currentTimeMillis()
}

data class CalendarFeed(
    val name: String,
    val source: String,
    val events: List<CalendarEvent>,
    val fetchedAt: Long = System.currentTimeMillis(),
)