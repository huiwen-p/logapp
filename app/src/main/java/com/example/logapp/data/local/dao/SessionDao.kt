package com.example.logapp.data.local.dao

import androidx.room.*
import com.example.logapp.data.local.entity.ActivitySessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {
    @Query("SELECT * FROM activity_sessions WHERE deletedAt IS NULL ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE endedAt IS NULL AND deletedAt IS NULL")
    fun getActiveSessions(): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE activityId = :activityId AND deletedAt IS NULL")
    fun getSessionsForActivity(activityId: String): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE startedAt >= :start AND startedAt < :end AND deletedAt IS NULL")
    fun getSessionsInRange(start: Instant, end: Instant): Flow<List<ActivitySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ActivitySessionEntity)

    @Update
    suspend fun update(session: ActivitySessionEntity)

    @Delete
    suspend fun delete(session: ActivitySessionEntity)
}
