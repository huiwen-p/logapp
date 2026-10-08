package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.data.local.entity.SyncStatus
import com.example.logapp.domain.backup.BackupSerializer
import com.example.logapp.domain.backup.ImportValidationEngine
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

enum class ImportStrategy {
    REPLACE, MERGE
}

class ImportDataUseCase @Inject constructor(
    private val backupSerializer: BackupSerializer,
    private val validationEngine: ImportValidationEngine,
    private val activityRepository: ActivityRepository,
    private val sessionRepository: SessionRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(jsonString: String, strategy: ImportStrategy) {
        val schema = backupSerializer.importFromJson(jsonString)
        validationEngine.validate(schema)

        transactionProvider.runAsTransaction {
            if (strategy == ImportStrategy.REPLACE) {
                // Clear existing data (but we should ideally do this gracefully by tracking SyncMetadata for deletions)
                // For REPLACE in a true local-first app, we just wipe and insert.
                // However, doing a hard wipe might mess up cloud sync.
                // For Phase 4, we'll implement MERGE as the default safe behavior.
                // If REPLACE is truly needed, we must generate DELETE sync metadata for all existing records.
                val existingActivities = activityRepository.getAllActivities().first()
                val existingSessions = sessionRepository.getAllSessions().first()
                
                for (activity in existingActivities) {
                    activityRepository.deleteActivity(activity)
                    syncMetadataRepository.insertMetadata(SyncMetadataEntity(activity.id, "ACTIVITY", "DELETE", activity.version, SyncStatus.PENDING))
                }
                for (session in existingSessions) {
                    sessionRepository.deleteSession(session)
                    syncMetadataRepository.insertMetadata(SyncMetadataEntity(session.id, "SESSION", "DELETE", session.version, SyncStatus.PENDING))
                }
            }

            // Upsert imported data
            for (backupActivity in schema.activities) {
                val entity = ActivityEntity(
                    id = backupActivity.id,
                    name = backupActivity.name,
                    icon = backupActivity.icon,
                    color = backupActivity.color,
                    categoryId = backupActivity.categoryId,
                    sortOrder = backupActivity.sortOrder,
                    isArchived = backupActivity.isArchived,
                    createdAt = backupActivity.createdAt,
                    updatedAt = backupActivity.updatedAt,
                    syncStatus = SyncStatus.PENDING,
                    version = backupActivity.version
                )
                // We use insert(onConflict=REPLACE) in DAO, so addActivity handles upsert.
                activityRepository.addActivity(entity)
                syncMetadataRepository.insertMetadata(SyncMetadataEntity(entity.id, "ACTIVITY", "INSERT", entity.version, SyncStatus.PENDING))
            }

            for (backupSession in schema.sessions) {
                val entity = ActivitySessionEntity(
                    id = backupSession.id,
                    activityId = backupSession.activityId,
                    startedAt = backupSession.startedAt,
                    endedAt = backupSession.endedAt,
                    note = backupSession.note,
                    createdAt = backupSession.createdAt,
                    updatedAt = backupSession.updatedAt,
                    deletedAt = backupSession.deletedAt,
                    syncStatus = SyncStatus.PENDING,
                    version = backupSession.version
                )
                sessionRepository.insertSession(entity)
                syncMetadataRepository.insertMetadata(SyncMetadataEntity(entity.id, "SESSION", "INSERT", entity.version, SyncStatus.PENDING))
            }
        }
    }
}
