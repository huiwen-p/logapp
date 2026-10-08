package com.example.logapp.data.repository

import com.example.logapp.data.local.dao.ActivityDao
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

class ActivityRepositoryImpl(
    private val activityDao: ActivityDao
) : ActivityRepository {
    override fun getActiveActivities(): Flow<List<ActivityEntity>> = activityDao.getActiveActivities()
    
    override fun getAllActivities(): Flow<List<ActivityEntity>> = activityDao.getAllActivities()
    
    override suspend fun getActivityById(id: String): ActivityEntity? = activityDao.getActivityById(id)
    
    override suspend fun addActivity(activity: ActivityEntity) = activityDao.insert(activity)
    
    override suspend fun updateActivity(activity: ActivityEntity) = activityDao.update(activity)
    
    override suspend fun deleteActivity(activity: ActivityEntity) = activityDao.delete(activity)
}
