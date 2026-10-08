package com.example.logapp.data.backup

import android.content.Context
import com.example.logapp.domain.backup.DriveBackupManager
import com.example.logapp.domain.repository.GoogleAuthRepository
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.client.http.ByteArrayContent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.time.Instant

class DriveBackupManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: GoogleAuthRepository
) : DriveBackupManager {

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    private fun getDriveService(): Drive {
        if (!authRepository.isSignedIn()) {
            throw IllegalStateException("User is not signed in to Google")
        }

        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("Google account not found")

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("LogApp")
            .build()
    }

    override suspend fun uploadBackupFile(jsonContent: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            
            // 1. Create/Find Folder "LogApp Backups"
            var folderId = findFolderId(driveService, "LogApp Backups")
            if (folderId == null) {
                folderId = createFolder(driveService, "LogApp Backups")
            }

            // 2. Upload File
            val fileName = "backup_${Instant.now().toEpochMilli()}.json"
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
                mimeType = "application/json"
            }
            
            val content = ByteArrayContent.fromString("application/json", jsonContent)
            val file = driveService.files().create(fileMetadata, content)
                .setFields("id")
                .execute()

            Result.success(file.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listBackups(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            val folderId = findFolderId(driveService, "LogApp Backups") ?: return@withContext Result.success(emptyList())

            val result = driveService.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setOrderBy("createdTime desc")
                .setFields("files(id, name, createdTime)")
                .execute()

            val files = result.files?.map { it.name } ?: emptyList()
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteOldBackups(keepCount: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            val folderId = findFolderId(driveService, "LogApp Backups") ?: return@withContext Result.success(Unit)

            val result = driveService.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setOrderBy("createdTime desc")
                .setFields("files(id, name, createdTime)")
                .execute()

            val files = result.files ?: emptyList()
            if (files.size > keepCount) {
                val filesToDelete = files.drop(keepCount)
                for (file in filesToDelete) {
                    driveService.files().delete(file.id).execute()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findFolderId(driveService: Drive, folderName: String): String? {
        val result = driveService.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
        
        return result.files?.firstOrNull()?.id
    }

    private fun createFolder(driveService: Drive, folderName: String): String {
        val fileMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val file = driveService.files().create(fileMetadata)
            .setFields("id")
            .execute()
        return file.id
    }
}
