package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.backup.BackupSerializer
import com.example.logapp.domain.backup.ImportValidationEngine
import com.example.logapp.domain.model.backup.BackupActivity
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.model.backup.BackupSession
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ImportDataUseCaseTest {

    private lateinit var backupSerializer: BackupSerializer
    private lateinit var validationEngine: ImportValidationEngine
    private lateinit var activityRepository: ActivityRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var syncMetadataRepository: SyncMetadataRepository
    private lateinit var transactionProvider: TransactionProvider
    private lateinit var importDataUseCase: ImportDataUseCase

    @Before
    fun setUp() {
        backupSerializer = mockk()
        validationEngine = mockk(relaxed = true)
        activityRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        syncMetadataRepository = mockk(relaxed = true)
        
        transactionProvider = object : TransactionProvider {
            override suspend fun <T> runAsTransaction(block: suspend () -> T): T = block()
        }

        importDataUseCase = ImportDataUseCase(
            backupSerializer,
            validationEngine,
            activityRepository,
            sessionRepository,
            syncMetadataRepository,
            transactionProvider
        )
    }

    @Test
    fun `import replace clears existing data and inserts new`() = runTest {
        val jsonString = "fake_json"
        val schema = BackupSchemaV1(
            version = 1,
            exportTime = Instant.now(),
            activities = listOf(BackupActivity("a1", "Test", "icon", 0xFF123456L, null, 0, false, Instant.now(), Instant.now(), 1)),
            sessions = listOf(BackupSession("s1", "a1", Instant.now(), null, null, Instant.now(), Instant.now(), null, 1))
        )
        
        val existingActivities = listOf(ActivityEntity("a2", "Old", "icon", 0xFF123456L, null, 0, false, Instant.now(), Instant.now(), com.example.logapp.data.local.entity.SyncStatus.SYNCED, 1))
        val existingSessions = listOf(ActivitySessionEntity("s2", "a2", Instant.now(), null, null, Instant.now(), Instant.now(), null, com.example.logapp.data.local.entity.SyncStatus.SYNCED, 1))

        every { backupSerializer.importFromJson(jsonString) } returns schema
        coEvery { activityRepository.getAllActivities() } returns flowOf(existingActivities)
        coEvery { sessionRepository.getAllSessions() } returns flowOf(existingSessions)

        importDataUseCase(jsonString, ImportStrategy.REPLACE)

        coVerify { validationEngine.validate(schema) }
        coVerify { activityRepository.deleteActivity(existingActivities[0]) }
        coVerify { sessionRepository.deleteSession(existingSessions[0]) }
        coVerify { activityRepository.addActivity(any()) }
        coVerify { sessionRepository.insertSession(any()) }
    }

    @Test
    fun `import merge only inserts new data without clearing existing`() = runTest {
        val jsonString = "fake_json"
        val schema = BackupSchemaV1(
            version = 1,
            exportTime = Instant.now(),
            activities = listOf(BackupActivity("a1", "Test", "icon", 0xFF123456L, null, 0, false, Instant.now(), Instant.now(), 1)),
            sessions = listOf(BackupSession("s1", "a1", Instant.now(), null, null, Instant.now(), Instant.now(), null, 1))
        )
        
        every { backupSerializer.importFromJson(jsonString) } returns schema

        importDataUseCase(jsonString, ImportStrategy.MERGE)

        coVerify { validationEngine.validate(schema) }
        coVerify(exactly = 0) { activityRepository.deleteActivity(any()) }
        coVerify(exactly = 0) { sessionRepository.deleteSession(any()) }
        coVerify { activityRepository.addActivity(any()) }
        coVerify { sessionRepository.insertSession(any()) }
    }
}
