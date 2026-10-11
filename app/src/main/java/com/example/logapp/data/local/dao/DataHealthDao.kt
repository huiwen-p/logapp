package com.example.logapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity

@Dao
interface DataHealthDao {

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getTotalActivitiesCount(): Int

    @Query("SELECT COUNT(*) FROM activity_sessions WHERE deletedAt IS NULL")
    suspend fun getTotalSessionsCount(): Int

    @Query("SELECT * FROM activity_sessions WHERE deletedAt IS NULL AND activityId NOT IN (SELECT id FROM activities)")
    suspend fun getOrphanSessions(): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE deletedAt IS NULL AND endedAt IS NOT NULL AND endedAt < startedAt")
    suspend fun getInvalidTimestampSessions(): List<ActivitySessionEntity>

    @Query("SELECT * FROM activities WHERE name = :name LIMIT 1")
    suspend fun getActivityByName(name: String): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Update
    suspend fun updateSessions(sessions: List<ActivitySessionEntity>)
}
