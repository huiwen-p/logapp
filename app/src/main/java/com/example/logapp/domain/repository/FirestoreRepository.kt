package com.example.logapp.domain.repository

import com.example.logapp.data.local.entity.ActivitySessionEntity

interface FirestoreRepository {
    suspend fun uploadSession(userId: String, session: ActivitySessionEntity): Result<Unit>
    suspend fun pullSessions(userId: String, lastSyncTime: Long): Result<List<ActivitySessionEntity>>
}
