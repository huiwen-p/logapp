package com.example.logapp.domain.usecase

import com.example.logapp.domain.backup.BackupSerializer
import com.example.logapp.domain.backup.CsvGenerator
import com.example.logapp.domain.model.backup.BackupActivity
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.model.backup.BackupSession
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

enum class ExportFormat {
    JSON, CSV
}

class ExportDataUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sessionRepository: SessionRepository,
    private val backupSerializer: BackupSerializer,
    private val csvGenerator: CsvGenerator
) {
    suspend operator fun invoke(format: ExportFormat): String {
        val activities = activityRepository.getAllActivities().first()
        val sessions = sessionRepository.getAllSessions().first()
        
        return when (format) {
            ExportFormat.CSV -> {
                csvGenerator.generateSessionsCsv(sessions, activities)
            }
            ExportFormat.JSON -> {
                val backupActivities = activities.map {
                    BackupActivity(
                        it.id, it.name, it.icon, it.color, it.categoryId,
                        it.sortOrder, it.isArchived, it.createdAt, it.updatedAt, it.version
                    )
                }
                
                val backupSessions = sessions.map {
                    BackupSession(
                        it.id, it.activityId, it.startedAt, it.endedAt, it.note,
                        it.createdAt, it.updatedAt, it.deletedAt, it.version
                    )
                }
                
                val schema = BackupSchemaV1(
                    version = 1,
                    exportTime = Instant.now(),
                    activities = backupActivities,
                    sessions = backupSessions
                )
                
                backupSerializer.exportToJson(schema)
            }
        }
    }
}
