package com.example.logapp.domain.usecase

import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.domain.backup.DriveBackupManager
import com.example.logapp.domain.repository.GoogleAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriveBackupUseCaseTest {

    private lateinit var exportDataUseCase: ExportDataUseCase
    private lateinit var driveBackupManager: DriveBackupManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var googleAuthRepository: GoogleAuthRepository

    private lateinit var useCase: DriveBackupUseCase

    @Before
    fun setUp() {
        exportDataUseCase = mockk()
        driveBackupManager = mockk()
        preferencesManager = mockk()
        googleAuthRepository = mockk()

        useCase = DriveBackupUseCase(
            exportDataUseCase = exportDataUseCase,
            driveBackupManager = driveBackupManager,
            preferencesManager = preferencesManager,
            googleAuthRepository = googleAuthRepository
        )
    }

    @Test
    fun `invoke fails when not signed in to Google Drive`() = runTest {
        every { googleAuthRepository.isSignedIn() } returns false

        val result = useCase()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { exportDataUseCase(any()) }
        coVerify(exactly = 0) { driveBackupManager.uploadBackupFile(any()) }
    }

    @Test
    fun `invoke succeeds and rotates backups when signed in and upload succeeds`() = runTest {
        every { googleAuthRepository.isSignedIn() } returns true
        coEvery { exportDataUseCase(ExportFormat.JSON) } returns "{\"test\": true}"
        coEvery { driveBackupManager.uploadBackupFile(any()) } returns Result.success("file-id-123")
        coEvery { preferencesManager.setLastBackupTime(any()) } returns Unit
        every { preferencesManager.backupKeepCount } returns flowOf(7)
        coEvery { driveBackupManager.deleteOldBackups(7) } returns Result.success(Unit)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { exportDataUseCase(ExportFormat.JSON) }
        coVerify(exactly = 1) { driveBackupManager.uploadBackupFile("{\"test\": true}") }
        coVerify(exactly = 1) { driveBackupManager.deleteOldBackups(7) }
    }

    @Test
    fun `invoke returns failure when upload fails`() = runTest {
        every { googleAuthRepository.isSignedIn() } returns true
        coEvery { exportDataUseCase(ExportFormat.JSON) } returns "{\"test\": true}"
        coEvery { driveBackupManager.uploadBackupFile(any()) } returns Result.failure(Exception("Upload network error"))
        every { preferencesManager.backupKeepCount } returns flowOf(7)

        val result = useCase()

        assertFalse(result.isSuccess)
        coVerify(exactly = 0) { driveBackupManager.deleteOldBackups(any()) }
    }
}
