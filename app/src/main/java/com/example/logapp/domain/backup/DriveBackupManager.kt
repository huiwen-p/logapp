package com.example.logapp.domain.backup

interface DriveBackupManager {
    suspend fun uploadBackupFile(jsonContent: String): Result<String>
    suspend fun listBackups(): Result<List<String>> // Returns list of file IDs or names
    suspend fun deleteOldBackups(keepCount: Int = 7): Result<Unit>
}
