package com.example.logapp.domain.model

import java.time.Instant

data class TimelineSegment(
    val start: Instant,
    val end: Instant,
    val activeSessionIds: Set<String>,
    val activeActivityIds: Set<String>
) {
    val durationMillis: Long
        get() = end.toEpochMilli() - start.toEpochMilli()
}
