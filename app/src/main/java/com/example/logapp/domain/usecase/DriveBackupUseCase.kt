package com.example.logapp.domain.usecase

import com.example.logapp.domain.backup.DriveBackupManager
import javax.inject.Inject

class DriveBackupUseCase @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val driveBackupManager: DriveBackupManager
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val jsonContent = exportDataUseCase(ExportFormat.JSON)
            val uploadResult = driveBackupManager.uploadBackupFile(jsonContent)
            
            if (uploadResult.isSuccess) {
                // Task-07.3: Backup Rotation (Delete older than 7 versions)
                driveBackupManager.deleteOldBackups(keepCount = 7)
                Result.success(Unit)
            } else {
                Result.failure(uploadResult.exceptionOrNull() ?: Exception("Unknown error uploading backup"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
