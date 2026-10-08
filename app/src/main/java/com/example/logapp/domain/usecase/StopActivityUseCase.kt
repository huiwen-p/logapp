package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.data.local.entity.SyncStatus
import com.example.logapp.domain.controller.NotificationController
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import javax.inject.Inject

class StopActivityUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider,
    private val notificationController: NotificationController
) {
    suspend operator fun invoke(sessionId: String) {
        val session = sessionRepository.getAllSessions().firstOrNull()?.find { it.id == sessionId }
        if (session != null) {
            invoke(session)
        }
    }
    suspend operator fun invoke(session: ActivitySessionEntity) {
        if (session.endedAt != null) return // Already stopped
        val updatedSession = session.copy(
            endedAt = Instant.now(),
            updatedAt = Instant.now(),
            version = session.version + 1
        )
        transactionProvider.runAsTransaction {
            sessionRepository.updateSession(updatedSession)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(
                    entityId = updatedSession.id,
                    entityType = "SESSION",
                    operation = "UPDATE",
                    localVersion = updatedSession.version,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        notificationController.hideActiveSessionNotification()
    }
}
