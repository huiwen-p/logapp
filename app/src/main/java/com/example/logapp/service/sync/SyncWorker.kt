package com.example.logapp.service.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.logapp.data.local.dao.SyncMetadataDao
import com.example.logapp.data.local.entity.SyncStatus
import com.example.logapp.domain.repository.AuthRepository
import com.example.logapp.domain.repository.FirestoreRepository
import com.example.logapp.domain.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncMetadataDao: SyncMetadataDao,
    private val sessionRepository: SessionRepository,
    private val firestoreRepository: FirestoreRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            // If user is not logged in, we try to log in anonymously
            val authResult = authRepository.signInAnonymously()
            if (authResult.isFailure) {
                return Result.retry()
            }
        }
        
        val actualUserId = authRepository.getCurrentUserId() ?: return Result.retry()

        val pendingSyncs = syncMetadataDao.getPendingSyncs()
        if (pendingSyncs.isEmpty()) {
            return Result.success()
        }

        var hasFailures = false

        for (metadata in pendingSyncs) {
            try {
                // Update status to SYNCING
                val syncingMetadata = metadata.copy(syncStatus = SyncStatus.SYNCING, lastAttemptAt = Instant.now())
                syncMetadataDao.update(syncingMetadata)

                if (metadata.entityType == "SESSION") {
                    val sessions = sessionRepository.getAllSessions().firstOrNull() ?: emptyList()
                    val session = sessions.find { it.id == metadata.entityId }

                    if (session != null) {
                        val result = firestoreRepository.uploadSession(actualUserId, session)
                        if (result.isSuccess) {
                            val syncedMetadata = syncingMetadata.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = Instant.now())
                            syncMetadataDao.update(syncedMetadata)
                        } else {
                            val failedMetadata = syncingMetadata.copy(syncStatus = SyncStatus.FAILED, error = result.exceptionOrNull()?.message)
                            syncMetadataDao.update(failedMetadata)
                            hasFailures = true
                        }
                    } else {
                        // Session not found locally, maybe deleted and not soft deleted?
                        // Just mark as synced to prevent infinite loop.
                        val syncedMetadata = syncingMetadata.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = Instant.now(), error = "Session not found locally")
                        syncMetadataDao.update(syncedMetadata)
                    }
                } else {
                    // Unhandled entity type, ignore for now
                    val syncedMetadata = syncingMetadata.copy(syncStatus = SyncStatus.SYNCED, error = "Unhandled entity type")
                    syncMetadataDao.update(syncedMetadata)
                }
            } catch (e: Exception) {
                val failedMetadata = metadata.copy(syncStatus = SyncStatus.FAILED, error = e.message, lastAttemptAt = Instant.now())
                syncMetadataDao.update(failedMetadata)
                hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }
}
