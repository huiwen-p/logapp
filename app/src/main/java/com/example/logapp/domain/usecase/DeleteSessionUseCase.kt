package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.data.local.entity.SyncStatus
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import java.time.Instant
import javax.inject.Inject

class DeleteSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(session: ActivitySessionEntity) {
        val updatedSession = session.copy(
            deletedAt = Instant.now(),
            updatedAt = Instant.now(),
            version = session.version + 1
        )
        transactionProvider.runAsTransaction {
            sessionRepository.updateSession(updatedSession) // Soft delete
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(
                    entityId = updatedSession.id,
                    entityType = "SESSION",
                    operation = "DELETE",
                    localVersion = updatedSession.version,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
    }
}
