package com.example.logapp.domain.engine

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.model.TimelineSegment
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class DailyAnalytics(
    val uniqueClockDuration: Duration,
    val trackedDuration: Duration,
    val overlapDuration: Duration,
    val concurrencyDistribution: Map<Int, Duration>,
    val timelineSegments: List<TimelineSegment>
)

object AnalyticsEngine {
    
    fun calculateDailyAnalytics(
        sessions: List<ActivitySessionEntity>,
        currentTime: Instant
    ): DailyAnalytics {
        val segments = OverlapEngine.calculateSegments(sessions, currentTime)
        
        val uniqueDuration = ConcurrencyEngine.calculateUniqueClockDuration(segments)
        
        // Sum of all individual session durations (including overlaps)
        val validSessions = sessions.map { 
            it.copy(endedAt = it.endedAt ?: currentTime) 
        }.filter { it.startedAt.isBefore(it.endedAt) }
        
        val trackedDurationMillis = validSessions.sumOf { 
            it.endedAt!!.toEpochMilli() - it.startedAt.toEpochMilli() 
        }
        val trackedDuration = trackedDurationMillis.milliseconds
        
        val overlapDuration = trackedDuration - uniqueDuration
        val concurrencyDistribution = ConcurrencyEngine.calculateConcurrencyDistribution(segments)
        
        return DailyAnalytics(
            uniqueClockDuration = uniqueDuration,
            trackedDuration = trackedDuration,
            overlapDuration = overlapDuration,
            concurrencyDistribution = concurrencyDistribution,
            timelineSegments = segments
        )
    }
}
