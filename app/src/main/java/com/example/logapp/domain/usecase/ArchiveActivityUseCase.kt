package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import javax.inject.Inject

class ArchiveActivityUseCase @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(activity: ActivityEntity): Result<Unit> {
        val archivedActivity = activity.copy(
            isArchived = true,
            updatedAt = java.time.Instant.now()
        )
        activityRepository.updateActivity(archivedActivity)
        return Result.success(Unit)
    }
}
