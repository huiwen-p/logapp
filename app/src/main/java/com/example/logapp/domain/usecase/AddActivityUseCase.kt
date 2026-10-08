package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import javax.inject.Inject

class AddActivityUseCase @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(activity: ActivityEntity): Result<Unit> {
        // Business Rule: name không được rỗng
        if (activity.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Tên hoạt động không được để trống."))
        }
        
        activityRepository.addActivity(activity)
        return Result.success(Unit)
    }
}
