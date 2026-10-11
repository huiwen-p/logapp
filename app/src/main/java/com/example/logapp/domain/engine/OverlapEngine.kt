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

        val startsByTime = mutableMapOf<Instant, MutableList<ActivitySessionEntity>>()
        val endsByTime = mutableMapOf<Instant, MutableList<ActivitySessionEntity>>()
        val uniquePoints = java.util.TreeSet<Instant>()

        for (session in validSessions) {
            val start = session.startedAt
            val end = session.endedAt!!
            uniquePoints.add(start)
            uniquePoints.add(end)
            startsByTime.getOrPut(start) { mutableListOf() }.add(session)
            endsByTime.getOrPut(end) { mutableListOf() }.add(session)
        }

        val sortedPoints = uniquePoints.toList()
        val segments = mutableListOf<TimelineSegment>()
        val activeSessions = mutableMapOf<String, ActivitySessionEntity>()

        for (i in 0 until sortedPoints.size - 1) {
            val p1 = sortedPoints[i]
            val p2 = sortedPoints[i + 1]

            // Process ends and starts at boundary point p1
            endsByTime[p1]?.forEach { activeSessions.remove(it.id) }
            startsByTime[p1]?.forEach { activeSessions[it.id] = it }

            if (activeSessions.isNotEmpty()) {
                val sessionIds = activeSessions.keys.toSet()
                val activityIds = activeSessions.values.map { it.activityId }.toSet()

                segments.add(
                    TimelineSegment(
                        start = p1,
                        end = p2,
                        activeSessionIds = sessionIds,
                        activeActivityIds = activityIds
                    )
                )
            }
        }
        return segments
    }
}
