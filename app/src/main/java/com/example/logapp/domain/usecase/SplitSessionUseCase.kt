package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.data.local.entity.SyncStatus
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SplitSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(session: ActivitySessionEntity, splitAt: Instant) {
        if (splitAt.isBefore(session.startedAt) || (session.endedAt != null && splitAt.isAfter(session.endedAt))) {
            throw IllegalArgumentException("Split time must be within session bounds")
        }

        val now = Instant.now()
        val session1 = session.copy(
            id = UUID.randomUUID().toString(),
            endedAt = splitAt,
            createdAt = now,
            updatedAt = now,
            version = 1
        )
        val session2 = session.copy(
            id = UUID.randomUUID().toString(),
            startedAt = splitAt,
            createdAt = now,
            updatedAt = now,
            version = 1
        )
        
        val deletedOldSession = session.copy(
            deletedAt = now,
            updatedAt = now,
            version = session.version + 1
        )

        transactionProvider.runAsTransaction {
            sessionRepository.updateSession(deletedOldSession)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(deletedOldSession.id, "SESSION", "DELETE", deletedOldSession.version, SyncStatus.PENDING)
            )

            sessionRepository.insertSession(session1)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(session1.id, "SESSION", "INSERT", session1.version, SyncStatus.PENDING)
            )

            sessionRepository.insertSession(session2)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(session2.id, "SESSION", "INSERT", session2.version, SyncStatus.PENDING)
            )
        }
    }
}
