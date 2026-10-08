package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.data.local.entity.SyncStatus
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.controller.NotificationController
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class StartActivityUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider,
    private val notificationController: NotificationController,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(activityId: String) {
        val now = Instant.now()
        val session = ActivitySessionEntity(
            id = UUID.randomUUID().toString(),
            activityId = activityId,
            startedAt = now,
            endedAt = null,
            note = null,
            createdAt = now,
            updatedAt = now,
            version = 1
        )
        transactionProvider.runAsTransaction {
            sessionRepository.insertSession(session)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(
                    entityId = session.id,
                    entityType = "SESSION",
                    operation = "INSERT",
                    localVersion = session.version,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        
        val activity = activityRepository.getAllActivities().firstOrNull()?.find { it.id == activityId }
        notificationController.showActiveSessionNotification(
            sessionId = session.id,
            activityName = activity?.name ?: "Unknown Activity",
            startedAt = session.startedAt.toEpochMilli()
        )
    }
}
