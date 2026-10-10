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

class MergeSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(session1: ActivitySessionEntity, session2: ActivitySessionEntity) {
        if (session1.activityId != session2.activityId) {
            throw IllegalArgumentException("Cannot merge sessions from different activities")
        }

        val (first, second) = if (session1.startedAt.isBefore(session2.startedAt)) {
            session1 to session2
        } else {
            session2 to session1
        }

        if (first.endedAt != null && first.endedAt.isBefore(second.startedAt)) {
            throw IllegalArgumentException("Cannot merge sessions that have a gap between them")
        }

        val now = Instant.now()
        val mergedStart = first.startedAt
        
        val end1 = session1.endedAt ?: now
        val end2 = session2.endedAt ?: now
        val mergedEnd = if (end1.isAfter(end2)) end1 else end2

        val mergedSession = session1.copy(
            id = UUID.randomUUID().toString(),
            startedAt = mergedStart,
            endedAt = if (session1.endedAt == null || session2.endedAt == null) null else mergedEnd,
            note = listOfNotNull(session1.note, session2.note).joinToString(" | ").takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
            version = 1
        )

        val deleted1 = session1.copy(deletedAt = now, updatedAt = now, version = session1.version + 1)
        val deleted2 = session2.copy(deletedAt = now, updatedAt = now, version = session2.version + 1)

        transactionProvider.runAsTransaction {
            sessionRepository.updateSession(deleted1)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(deleted1.id, "SESSION", "DELETE", deleted1.version, SyncStatus.PENDING)
            )

            sessionRepository.updateSession(deleted2)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(deleted2.id, "SESSION", "DELETE", deleted2.version, SyncStatus.PENDING)
            )

            sessionRepository.insertSession(mergedSession)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(mergedSession.id, "SESSION", "INSERT", mergedSession.version, SyncStatus.PENDING)
            )
        }
    }
}
