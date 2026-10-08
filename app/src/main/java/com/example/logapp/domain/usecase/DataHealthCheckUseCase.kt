package com.example.logapp.domain.usecase

import com.example.logapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

data class HealthCheckReport(
    val totalSessions: Int,
    val totalAnomalies: Int,
    val anomalies: List<String>
)

class DataHealthCheckUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): HealthCheckReport {
        val anomalies = mutableListOf<String>()
        val sessions = sessionRepository.getAllSessions().firstOrNull() ?: emptyList()
        
        var totalAnomalies = 0
        
        sessions.forEach { session ->
            // Check 1: Missing end time for a very old session (e.g. active for more than 24 hours)
            if (session.endedAt == null) {
                val now = java.time.Instant.now().toEpochMilli()
                val startedAt = session.startedAt.toEpochMilli()
                if (now - startedAt > 24 * 60 * 60 * 1000) {
                    anomalies.add("Session ${session.id} has been active for > 24h.")
                    totalAnomalies++
                }
            }
            
            // Check 2: End time is before start time
            if (session.endedAt != null && session.endedAt.isBefore(session.startedAt)) {
                anomalies.add("Session ${session.id} has end time before start time.")
                totalAnomalies++
            }
        }
        
        // Check 3: Overlaps
        val sortedSessions = sessions.filter { it.endedAt != null }.sortedBy { it.startedAt }
        for (i in 0 until sortedSessions.size - 1) {
            val current = sortedSessions[i]
            val next = sortedSessions[i + 1]
            if (current.endedAt != null && current.endedAt.isAfter(next.startedAt)) {
                anomalies.add("Session ${current.id} overlaps with ${next.id}")
                totalAnomalies++
            }
        }
        
        return HealthCheckReport(
            totalSessions = sessions.size,
            totalAnomalies = totalAnomalies,
            anomalies = anomalies
        )
    }
}
