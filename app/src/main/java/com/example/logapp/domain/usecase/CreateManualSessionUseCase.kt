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

class CreateManualSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(activityId: String, startTime: Instant, endTime: Instant, note: String?) {
        if (endTime.isBefore(startTime)) {
            throw IllegalArgumentException("End time must be after or equal to start time")
        }

        val sessionId = UUID.randomUUID().toString()
        val session = ActivitySessionEntity(
            id = sessionId,
            activityId = activityId,
            startedAt = startTime,
            endedAt = endTime,
            note = note,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 1
        )

        transactionProvider.runAsTransaction {
            sessionRepository.insertSession(session)
            syncMetadataRepository.insertMetadata(
                SyncMetadataEntity(
                    entityId = sessionId,
                    entityType = "SESSION",
                    operation = "INSERT",
                    localVersion = 1,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
    }
}
