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
    val activityDurations: Map<String, Duration>,
    val activityRelationships: Map<Pair<String, String>, Duration>,
    val timelineSegments: List<TimelineSegment>
)

data class WeeklyAnalytics(
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val totalUniqueDuration: Duration,
    val totalTrackedDuration: Duration,
    val dailyBreakdown: Map<java.time.LocalDate, DailyAnalytics>,
    val activityDurations: Map<String, Duration>,
    val activityRelationships: Map<Pair<String, String>, Duration>
)

data class MonthlyAnalytics(
    val yearMonth: java.time.YearMonth,
    val totalUniqueDuration: Duration,
    val totalTrackedDuration: Duration,
    val dailyBreakdown: Map<java.time.LocalDate, DailyAnalytics>,
    val activityDurations: Map<String, Duration>,
    val activityRelationships: Map<Pair<String, String>, Duration>
)

object AnalyticsEngine {
    
    fun calculateDailyAnalytics(
        sessions: List<ActivitySessionEntity>,
        currentTime: Instant,
        boundaryStart: Instant = Instant.MIN,
        boundaryEnd: Instant = Instant.MAX
    ): DailyAnalytics {
        val validSessions = sessions.mapNotNull { session ->
            val e = session.endedAt ?: currentTime
            val s = session.startedAt
            
            if (e.isBefore(boundaryStart) || s.isAfter(boundaryEnd)) return@mapNotNull null
            
            val truncatedStart = if (s.isBefore(boundaryStart)) boundaryStart else s
            val truncatedEnd = if (e.isAfter(boundaryEnd)) boundaryEnd else e
            
            if (truncatedStart.isBefore(truncatedEnd)) {
                session.copy(startedAt = truncatedStart, endedAt = truncatedEnd)
            } else null
        }
        
        val segments = OverlapEngine.calculateSegments(validSessions, currentTime)
        
        val uniqueDuration = ConcurrencyEngine.calculateUniqueClockDuration(segments)
        
        val activityDurations = mutableMapOf<String, Duration>()
        segments.forEach { segment ->
            val duration = segment.durationMillis.milliseconds
            segment.activeActivityIds.forEach { activityId ->
                activityDurations[activityId] = (activityDurations[activityId] ?: Duration.ZERO) + duration
            }
        }
        
        val trackedDuration = activityDurations.values.fold(Duration.ZERO) { acc, d -> acc + d }
        val overlapDuration = trackedDuration - uniqueDuration
        val concurrencyDistribution = ConcurrencyEngine.calculateConcurrencyDistribution(segments)
            
        val relationships = mutableMapOf<Pair<String, String>, Duration>()
        segments.forEach { segment ->
            val activeIds = segment.activeActivityIds.toList()
            if (activeIds.size >= 2) {
                val duration = segment.durationMillis.milliseconds
                for (i in 0 until activeIds.size) {
                    for (j in i + 1 until activeIds.size) {
                        val a = activeIds[i]
                        val b = activeIds[j]
                        val pair = if (a < b) Pair(a, b) else Pair(b, a)
                        relationships[pair] = (relationships[pair] ?: Duration.ZERO) + duration
                    }
                }
            }
        }
        
        return DailyAnalytics(
            uniqueClockDuration = uniqueDuration,
            trackedDuration = trackedDuration,
            overlapDuration = overlapDuration,
            concurrencyDistribution = concurrencyDistribution,
            activityDurations = activityDurations,
            activityRelationships = relationships,
            timelineSegments = segments
        )
    }

    fun calculateWeeklyAnalytics(
        sessions: List<ActivitySessionEntity>,
        currentTime: Instant,
        dailyBoundaries: Map<java.time.LocalDate, Pair<Instant, Instant>>
    ): WeeklyAnalytics {
        val dailyBreakdown = dailyBoundaries.mapValues { (_, bounds) ->
            calculateDailyAnalytics(sessions, currentTime, bounds.first, bounds.second)
        }
        
        val totalUniqueDuration = dailyBreakdown.values.map { it.uniqueClockDuration }.fold(Duration.ZERO) { acc, d -> acc + d }
        val totalTrackedDuration = dailyBreakdown.values.map { it.trackedDuration }.fold(Duration.ZERO) { acc, d -> acc + d }
        
        val activityDurations = mutableMapOf<String, Duration>()
        val activityRelationships = mutableMapOf<Pair<String, String>, Duration>()
        dailyBreakdown.values.forEach { daily ->
            daily.activityDurations.forEach { (id, duration) ->
                activityDurations[id] = (activityDurations[id] ?: Duration.ZERO) + duration
            }
            daily.activityRelationships.forEach { (pair, duration) ->
                activityRelationships[pair] = (activityRelationships[pair] ?: Duration.ZERO) + duration
            }
        }
        
        val sortedDates = dailyBoundaries.keys.sorted()
        
        return WeeklyAnalytics(
            startDate = sortedDates.firstOrNull() ?: java.time.LocalDate.now(),
            endDate = sortedDates.lastOrNull() ?: java.time.LocalDate.now(),
            totalUniqueDuration = totalUniqueDuration,
            totalTrackedDuration = totalTrackedDuration,
            dailyBreakdown = dailyBreakdown,
            activityDurations = activityDurations,
            activityRelationships = activityRelationships
        )
    }

    fun calculateMonthlyAnalytics(
        sessions: List<ActivitySessionEntity>,
        currentTime: Instant,
        yearMonth: java.time.YearMonth,
        dailyBoundaries: Map<java.time.LocalDate, Pair<Instant, Instant>>
    ): MonthlyAnalytics {
        val dailyBreakdown = dailyBoundaries.mapValues { (_, bounds) ->
            calculateDailyAnalytics(sessions, currentTime, bounds.first, bounds.second)
        }
        
        val totalUniqueDuration = dailyBreakdown.values.map { it.uniqueClockDuration }.fold(Duration.ZERO) { acc, d -> acc + d }
        val totalTrackedDuration = dailyBreakdown.values.map { it.trackedDuration }.fold(Duration.ZERO) { acc, d -> acc + d }
        
        val activityDurations = mutableMapOf<String, Duration>()
        val activityRelationships = mutableMapOf<Pair<String, String>, Duration>()
        dailyBreakdown.values.forEach { daily ->
            daily.activityDurations.forEach { (id, duration) ->
                activityDurations[id] = (activityDurations[id] ?: Duration.ZERO) + duration
            }
            daily.activityRelationships.forEach { (pair, duration) ->
                activityRelationships[pair] = (activityRelationships[pair] ?: Duration.ZERO) + duration
            }
        }
        
        return MonthlyAnalytics(
            yearMonth = yearMonth,
            totalUniqueDuration = totalUniqueDuration,
            totalTrackedDuration = totalTrackedDuration,
            dailyBreakdown = dailyBreakdown,
            activityDurations = activityDurations,
            activityRelationships = activityRelationships
        )
    }
}
