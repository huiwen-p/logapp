package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteActivityUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(activity: ActivityEntity): Result<Unit> {
        // Business Rule: Không được xóa Activity nếu làm mất session lịch sử. 
        val sessions = sessionRepository.getSessionsForActivity(activity.id).first()
        
        if (sessions.isNotEmpty()) {
            return Result.failure(
                IllegalStateException("Không thể xoá Activity vì đã có lịch sử session. Hãy dùng tính năng Archive để thay thế.")
            )
        }
        
        activityRepository.deleteActivity(activity)
        return Result.success(Unit)
    }
}
