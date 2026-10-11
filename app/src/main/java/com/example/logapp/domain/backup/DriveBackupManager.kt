package com.example.logapp.domain.backup

data class DriveBackupFile(val id: String, val name: String, val createdTime: Long)

interface DriveBackupManager {
    suspend fun uploadBackupFile(jsonContent: String): Result<String>
    suspend fun listBackups(): Result<List<DriveBackupFile>>
    suspend fun downloadBackup(fileId: String): Result<String>
    suspend fun deleteOldBackups(keepCount: Int = 7): Result<Unit>
}
