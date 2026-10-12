package com.example.logapp.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class TimelineSegment(
    val start: Instant,
    val end: Instant,
    val activeSessionIds: Set<String>,
    val activeActivityIds: Set<String>
) {
    val durationMillis: Long
        get() = end.toEpochMilli() - start.toEpochMilli()
}
