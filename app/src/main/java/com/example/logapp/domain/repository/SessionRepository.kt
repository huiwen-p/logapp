package com.example.logapp.domain.repository

import com.example.logapp.data.local.entity.ActivitySessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SessionRepository {
    fun getAllSessions(): Flow<List<ActivitySessionEntity>>
    fun getActiveSessions(): Flow<List<ActivitySessionEntity>>
    fun getSessionsForActivity(activityId: String): Flow<List<ActivitySessionEntity>>
    fun getSessionsInRange(start: Instant, end: Instant): Flow<List<ActivitySessionEntity>>
    suspend fun insertSession(session: ActivitySessionEntity)
    suspend fun updateSession(session: ActivitySessionEntity)
    suspend fun deleteSession(session: ActivitySessionEntity)
}
