package com.example.logapp.feature.timeline.engine

import androidx.compose.runtime.Immutable
import com.example.logapp.data.local.entity.ActivitySessionEntity
import java.time.Instant

/**
 * Layout information for a session on the timeline canvas.
 */
@Immutable
data class SessionLayoutPosition(
    val session: ActivitySessionEntity,
    val columnIndex: Int,
    val totalColumns: Int,
    val isOverlapping: Boolean
)

/**
 * Cluster-based column allocation engine for Timeline canvas.
 *
 * Ensures maximum information density:
 * - Isolated sessions (no overlap) take 100% of the available timeline width.
 * - Overlapping sessions are grouped into localized clusters, where width is divided
 *   only among concurrently active tracks within that cluster.
 */
object TimelineLayoutEngine {

    /**
     * Calculates column positions and overlap status for a list of sessions.
     *
     * @param sessions List of sessions for the selected day.
     * @param referenceTime Current time used for ongoing sessions (endedAt == null).
     * @return Map of sessionId -> SessionLayoutPosition
     */
    fun calculateLayout(
        sessions: List<ActivitySessionEntity>,
        referenceTime: Instant = Instant.now()
    ): Map<String, SessionLayoutPosition> {
        if (sessions.isEmpty()) return emptyMap()

        val sortedSessions = sessions.sortedBy { it.startedAt }
        val result = mutableMapOf<String, SessionLayoutPosition>()

        // 1. Group sessions into contiguous overlap clusters
        val clusters = mutableListOf<MutableList<ActivitySessionEntity>>()
        var currentCluster = mutableListOf<ActivitySessionEntity>()
        var clusterMaxEnd = Instant.MIN

        for (session in sortedSessions) {
            val sessionEnd = session.endedAt ?: referenceTime

            if (currentCluster.isEmpty()) {
                currentCluster.add(session)
                clusterMaxEnd = sessionEnd
            } else {
                // If this session starts strictly before the cluster's max end time,
                // it is part of this connected component/cluster.
                if (session.startedAt.isBefore(clusterMaxEnd)) {
                    currentCluster.add(session)
                    if (sessionEnd.isAfter(clusterMaxEnd)) {
                        clusterMaxEnd = sessionEnd
                    }
                } else {
                    // Start a new cluster
                    clusters.add(currentCluster)
                    currentCluster = mutableListOf(session)
                    clusterMaxEnd = sessionEnd
                }
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        // 2. Allocate columns per cluster
        for (cluster in clusters) {
            val columns = mutableListOf<MutableList<ActivitySessionEntity>>()
            val sessionColMap = mutableMapOf<String, Int>()

            for (session in cluster) {
                var placed = false
                for (colIdx in 0 until columns.size) {
                    val lastInCol = columns[colIdx].last()
                    val lastEnd = lastInCol.endedAt ?: referenceTime
                    if (!session.startedAt.isBefore(lastEnd)) {
                        columns[colIdx].add(session)
                        sessionColMap[session.id] = colIdx
                        placed = true
                        break
                    }
                }
                if (!placed) {
                    columns.add(mutableListOf(session))
                    sessionColMap[session.id] = columns.size - 1
                }
            }

            val totalClusterColumns = maxOf(1, columns.size)

            for (session in cluster) {
                val colIndex = sessionColMap[session.id] ?: 0
                val overlapsWithAny = if (totalClusterColumns <= 1) {
                    false
                } else {
                    cluster.any { other ->
                        if (other.id == session.id) return@any false
                        val sEnd = session.endedAt ?: referenceTime
                        val oEnd = other.endedAt ?: referenceTime
                        session.startedAt.isBefore(oEnd) && other.startedAt.isBefore(sEnd)
                    }
                }

                result[session.id] = SessionLayoutPosition(
                    session = session,
                    columnIndex = colIndex,
                    totalColumns = totalClusterColumns,
                    isOverlapping = overlapsWithAny
                )
            }
        }

        return result
    }
}
