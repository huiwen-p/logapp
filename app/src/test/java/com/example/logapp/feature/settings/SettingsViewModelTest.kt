package com.example.logapp.feature.settings

import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.domain.backup.DriveBackupFile
import com.example.logapp.domain.backup.DriveBackupManager
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.repository.GoogleAuthRepository
import com.example.logapp.domain.usecase.ExportDataUseCase
import com.example.logapp.domain.usecase.ImportDataUseCase
import com.example.logapp.service.backup.DriveBackupScheduler
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var googleAuthRepository: GoogleAuthRepository
    private lateinit var exportDataUseCase: ExportDataUseCase
    private lateinit var importDataUseCase: ImportDataUseCase
    private lateinit var driveBackupManager: DriveBackupManager
    private lateinit var driveBackupUseCase: com.example.logapp.domain.usecase.DriveBackupUseCase
    private lateinit var checkDataHealthUseCase: com.example.logapp.domain.usecase.CheckDataHealthUseCase
    private lateinit var repairDataHealthUseCase: com.example.logapp.domain.usecase.RepairDataHealthUseCase
    private lateinit var syncMetadataRepository: com.example.logapp.domain.repository.SyncMetadataRepository

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(DriveBackupScheduler)
        every { DriveBackupScheduler.schedulePeriodicBackup(any(), any()) } just Runs
        every { DriveBackupScheduler.disablePeriodicBackup(any()) } just Runs

        preferencesManager = mockk(relaxed = true) {
            every { themeMode } returns flowOf("system")
            every { dayBoundary } returns flowOf("00:00")
            every { autoBackupEnabled } returns flowOf(false)
            every { backupFrequency } returns flowOf("daily")
            every { backupKeepCount } returns flowOf(7)
            every { lastBackupTime } returns flowOf(null)
        }
        googleAuthRepository = mockk(relaxed = true) {
            every { isSignedIn() } returns true
            every { getSignedInEmail() } returns "test@example.com"
        }
        exportDataUseCase = mockk(relaxed = true)
        importDataUseCase = mockk(relaxed = true)
        driveBackupManager = mockk(relaxed = true)
        driveBackupUseCase = mockk(relaxed = true)
        checkDataHealthUseCase = mockk(relaxed = true)
        repairDataHealthUseCase = mockk(relaxed = true)
        syncMetadataRepository = mockk(relaxed = true) {
            coEvery { getPendingSyncs() } returns emptyList()
        }

        viewModel = SettingsViewModel(
            preferencesManager,
            googleAuthRepository,
            exportDataUseCase,
            importDataUseCase,
            driveBackupManager,
            driveBackupUseCase,
            checkDataHealthUseCase,
            repairDataHealthUseCase,
            syncMetadataRepository
        )
    }

    @After
    fun tearDown() {
        unmockkObject(DriveBackupScheduler)
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDriveBackups updates driveBackups state on success`() = runTest(testDispatcher) {
        val backups = listOf(
            DriveBackupFile("id1", "backup_1.json", 1000L),
            DriveBackupFile("id2", "backup_2.json", 2000L)
        )
        coEvery { driveBackupManager.listBackups() } returns Result.success(backups)

        viewModel.loadDriveBackups()
        advanceUntilIdle()

        assertEquals(backups, viewModel.driveBackups.value)
        assertEquals(false, viewModel.isLoadingDriveBackups.value)
    }

    @Test
    fun `onDriveBackupSelectedForImport downloads file and updates preview`() = runTest(testDispatcher) {
        val fileId = "test_drive_file_id"
        val json = """{"version":1,"exportTime":"2026-10-11T00:00:00Z","activities":[],"sessions":[]}"""
        val schema = BackupSchemaV1(version = 1, exportTime = Instant.now(), activities = emptyList(), sessions = emptyList())

        coEvery { driveBackupManager.downloadBackup(fileId) } returns Result.success(json)
        coEvery { importDataUseCase.previewImport(json) } returns schema

        viewModel.onDriveBackupSelectedForImport(fileId)
        advanceUntilIdle()

        assertNotNull(viewModel.importPreviewData.value)
        assertEquals(schema, viewModel.importPreviewData.value)
    }

    @Test
    fun `autoBackup settings call preferencesManager`() = runTest(testDispatcher) {
        val context = mockk<android.content.Context>(relaxed = true)

        viewModel.setAutoBackupEnabled(context, true)
        advanceUntilIdle()
        io.mockk.coVerify { preferencesManager.setAutoBackupEnabled(true) }

        viewModel.setBackupFrequency(context, "weekly")
        advanceUntilIdle()
        io.mockk.coVerify { preferencesManager.setBackupFrequency("weekly") }

        viewModel.setBackupKeepCount(30)
        advanceUntilIdle()
        io.mockk.coVerify { preferencesManager.setBackupKeepCount(30) }
    }

    @Test
    fun `triggerBackupNow success updates backupMessage and reloads backups`() = runTest(testDispatcher) {
        coEvery { driveBackupUseCase() } returns Result.success(Unit)
        coEvery { driveBackupManager.listBackups() } returns Result.success(emptyList())

        viewModel.triggerBackupNow()
        advanceUntilIdle()

        io.mockk.coVerify { driveBackupUseCase() }
        assertEquals("Sao lưu lên Google Drive thành công!", viewModel.backupMessage.value)
        assertEquals(false, viewModel.isBackingUp.value)
    }

    @Test
    fun `signedInEmail reflects repository email`() = runTest(testDispatcher) {
        assertEquals("test@example.com", viewModel.signedInEmail.value)
    }

    @Test
    fun `checkDataHealth updates dataHealthReport`() = runTest(testDispatcher) {
        val report = com.example.logapp.domain.model.health.DataHealthReport(
            totalActivities = 10,
            totalSessions = 50,
            orphanSessionsCount = 0,
            invalidTimestampCount = 0
        )
        coEvery { checkDataHealthUseCase() } returns report

        viewModel.checkDataHealth()
        advanceUntilIdle()

        assertEquals(report, viewModel.dataHealthReport.value)
        assertEquals(false, viewModel.isCheckingHealth.value)
    }

    @Test
    fun `repairDataHealth calls repairUseCase and updates dataHealthReport`() = runTest(testDispatcher) {
        val repairedReport = com.example.logapp.domain.model.health.DataHealthReport(
            totalActivities = 11,
            totalSessions = 50,
            orphanSessionsCount = 0,
            invalidTimestampCount = 0
        )
        coEvery { repairDataHealthUseCase() } returns repairedReport

        viewModel.repairDataHealth()
        advanceUntilIdle()

        io.mockk.coVerify { repairDataHealthUseCase() }
        assertEquals(repairedReport, viewModel.dataHealthReport.value)
        assertEquals(false, viewModel.isRepairingHealth.value)
    }

    @Test
    fun `refreshPendingSyncs queries syncMetadataRepository and updates count`() = runTest(testDispatcher) {
        val dummyMetadata = listOf(
            com.example.logapp.data.local.entity.SyncMetadataEntity(
                entityId = "1",
                entityType = "ACTIVITY",
                operation = "INSERT",
                localVersion = 1,
                syncStatus = com.example.logapp.data.local.entity.SyncStatus.PENDING
            )
        )
        coEvery { syncMetadataRepository.getPendingSyncs() } returns dummyMetadata

        viewModel.refreshPendingSyncs()
        advanceUntilIdle()

        assertEquals(1, viewModel.pendingSyncCount.value)
    }
}
