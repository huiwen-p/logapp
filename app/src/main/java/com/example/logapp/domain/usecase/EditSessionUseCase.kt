package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.data.local.entity.SyncStatus
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import java.time.Instant
import javax.inject.Inject

class EditSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(session: ActivitySessionEntity, newStartTime: Instant, newEndTime: Instant?, newNote: String?) {
        val updatedSession = session.copy(
            startedAt = newStartTime,
            endedAt = newEndTime,
            note = newNote,
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
    }
}
