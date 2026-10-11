package com.example.logapp.domain.usecase

import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.domain.backup.DriveBackupManager
import com.example.logapp.domain.repository.GoogleAuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DriveBackupUseCase @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val driveBackupManager: DriveBackupManager,
    private val preferencesManager: PreferencesManager,
    private val googleAuthRepository: GoogleAuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            if (!googleAuthRepository.isSignedIn()) {
                return Result.failure(IllegalStateException("Not signed in to Google Drive"))
            }

            val jsonContent = exportDataUseCase(ExportFormat.JSON)
            val uploadResult = driveBackupManager.uploadBackupFile(jsonContent)
            
            if (uploadResult.isSuccess) {
                preferencesManager.setLastBackupTime(java.time.Instant.now().toString())
                // SRS #38: Backup Rotation (Keep configured number of backups, default 7)
                val keepCount = preferencesManager.backupKeepCount.first()
                driveBackupManager.deleteOldBackups(keepCount = keepCount)
                Result.success(Unit)
            } else {
                val ex = uploadResult.exceptionOrNull() ?: Exception("Unknown error uploading backup")
                Result.failure(ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
