package com.example.logapp.data.repository

import com.example.logapp.data.model.ActivityEntity
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun getActiveActivities(): Flow<List<ActivityEntity>>
    fun getAllActivities(): Flow<List<ActivityEntity>>
    suspend fun getActivityById(id: String): ActivityEntity?
    suspend fun addActivity(activity: ActivityEntity)
    suspend fun updateActivity(activity: ActivityEntity)
    suspend fun deleteActivity(activity: ActivityEntity)
}
