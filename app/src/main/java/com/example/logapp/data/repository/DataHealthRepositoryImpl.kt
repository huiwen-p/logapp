package com.example.logapp.data.repository

import com.example.logapp.data.local.dao.DataHealthDao
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.model.health.DataHealthReport
import com.example.logapp.domain.repository.DataHealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class DataHealthRepositoryImpl @Inject constructor(
    private val dataHealthDao: DataHealthDao
) : DataHealthRepository {

    override suspend fun checkHealth(): DataHealthReport = withContext(Dispatchers.IO) {
        val totalActivities = dataHealthDao.getTotalActivitiesCount()
        val totalSessions = dataHealthDao.getTotalSessionsCount()
        val orphanSessions = dataHealthDao.getOrphanSessions()
        val invalidTimestamps = dataHealthDao.getInvalidTimestampSessions()

        DataHealthReport(
            totalActivities = totalActivities,
            totalSessions = totalSessions,
            orphanSessionsCount = orphanSessions.size,
            invalidTimestampCount = invalidTimestamps.size,
            checkedAt = Instant.now()
        )
    }

    override suspend fun repairHealth(): DataHealthReport = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val sessionsToUpdate = mutableMapOf<String, ActivitySessionEntity>()

        // 1. Repair orphan sessions: link to "Chưa phân loại" activity
        val orphanSessions = dataHealthDao.getOrphanSessions()
        if (orphanSessions.isNotEmpty()) {
            val fallbackActivityName = "Chưa phân loại"
            var fallbackActivity = dataHealthDao.getActivityByName(fallbackActivityName)
            if (fallbackActivity == null) {
                fallbackActivity = ActivityEntity(
                    id = UUID.randomUUID().toString(),
                    name = fallbackActivityName,
                    icon = "help_outline",
                    color = 0xFF78909CL, // Blue-grey
                    sortOrder = 999,
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now
                )
                dataHealthDao.insertActivity(fallbackActivity)
            }

            orphanSessions.forEach { session ->
                sessionsToUpdate[session.id] = session.copy(
                    activityId = fallbackActivity.id,
                    updatedAt = now
                )
            }
        }

        // 2. Repair invalid timestamp sessions: swap startedAt and endedAt if endedAt < startedAt
        val invalidTimestamps = dataHealthDao.getInvalidTimestampSessions()
        invalidTimestamps.forEach { session ->
            val current = sessionsToUpdate[session.id] ?: session
            if (current.endedAt != null && current.endedAt.isBefore(current.startedAt)) {
                sessionsToUpdate[current.id] = current.copy(
                    startedAt = current.endedAt,
                    endedAt = current.startedAt,
                    updatedAt = now
                )
            }
        }

        if (sessionsToUpdate.isNotEmpty()) {
            dataHealthDao.updateSessions(sessionsToUpdate.values.toList())
        }

        checkHealth()
    }
}
