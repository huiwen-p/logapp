package com.example.logapp.domain.engine

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.model.TimelineSegment
import java.time.Instant

object OverlapEngine {

    /**
     * Splits a list of sessions into non-overlapping, continuous timeline segments.
     * @param sessions List of sessions to calculate.
     * @param currentTime Current time used to close currently active sessions.
     */
    fun calculateSegments(
        sessions: List<ActivitySessionEntity>,
        currentTime: Instant
    ): List<TimelineSegment> {
        // Treat active sessions as ending at currentTime and filter out zero/negative length sessions
        val validSessions = sessions.map { 
            it.copy(endedAt = it.endedAt ?: currentTime) 
        }.filter { it.startedAt.isBefore(it.endedAt) }

        if (validSessions.isEmpty()) return emptyList()

        // Extract all boundary points
        val points = mutableSetOf<Instant>()
        validSessions.forEach {
            points.add(it.startedAt)
            points.add(it.endedAt!!)
        }

        val sortedPoints = points.sorted()
        val segments = mutableListOf<TimelineSegment>()

        // Iterate through each interval between consecutive boundary points
        for (i in 0 until sortedPoints.size - 1) {
            val p1 = sortedPoints[i]
            val p2 = sortedPoints[i + 1]

            // Find sessions that overlap with the interval [p1, p2]
            val activeSessions = validSessions.filter {
                !it.startedAt.isAfter(p1) && !it.endedAt!!.isBefore(p2)
            }

            if (activeSessions.isNotEmpty()) {
                segments.add(
                    TimelineSegment(
                        start = p1,
                        end = p2,
                        activeSessionIds = activeSessions.map { it.id }.toSet(),
                        activeActivityIds = activeSessions.map { it.activityId }.toSet()
                    )
                )
            }
        }
        return segments
    }
}
