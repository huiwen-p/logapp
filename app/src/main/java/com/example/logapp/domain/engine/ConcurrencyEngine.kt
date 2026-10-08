package com.example.logapp.domain.engine

import com.example.logapp.domain.model.TimelineSegment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object ConcurrencyEngine {

    /**
     * Calculates total time spent multitasking across different concurrency levels (1, 2, 3, 4+ activities).
     * @return Map where key is concurrency level and value is the total duration.
     */
    fun calculateConcurrencyDistribution(segments: List<TimelineSegment>): Map<Int, Duration> {
        val distribution = mutableMapOf<Int, Long>()
        
        segments.forEach { segment ->
            val concurrencyLevel = segment.activeActivityIds.size
            if (concurrencyLevel > 0) {
                // Group 4 or more activities into level 4
                val level = if (concurrencyLevel >= 4) 4 else concurrencyLevel
                val current = distribution.getOrDefault(level, 0L)
                distribution[level] = current + segment.durationMillis
            }
        }
        
        return distribution.mapValues { it.value.milliseconds }
    }
    
    /**
     * Calculates the true wall-clock time passed (Unique Clock Duration), 
     * without double-counting overlapping sessions.
     */
    fun calculateUniqueClockDuration(segments: List<TimelineSegment>): Duration {
        val uniqueMillis = segments
            .filter { it.activeActivityIds.isNotEmpty() }
            .sumOf { it.durationMillis }
        return uniqueMillis.milliseconds
    }
}
