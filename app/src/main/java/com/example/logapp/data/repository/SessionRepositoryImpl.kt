package com.example.logapp.data.repository

import com.example.logapp.data.local.dao.SessionDao
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {
    override fun getAllSessions(): Flow<List<ActivitySessionEntity>> = sessionDao.getAllSessions()
    
    override fun getActiveSessions(): Flow<List<ActivitySessionEntity>> = sessionDao.getActiveSessions()
    
    override fun getSessionsForActivity(activityId: String): Flow<List<ActivitySessionEntity>> = sessionDao.getSessionsForActivity(activityId)
    
    override fun getSessionsInRange(start: Instant, end: Instant): Flow<List<ActivitySessionEntity>> = sessionDao.getSessionsInRange(start, end)
    
    override suspend fun insertSession(session: ActivitySessionEntity) = sessionDao.insert(session)
    
    override suspend fun updateSession(session: ActivitySessionEntity) = sessionDao.update(session)
    
    override suspend fun deleteSession(session: ActivitySessionEntity) = sessionDao.delete(session)
}
