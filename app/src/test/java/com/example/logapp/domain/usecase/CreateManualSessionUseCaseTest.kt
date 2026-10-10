package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class CreateManualSessionUseCaseTest {

    class FakeSessionRepository : SessionRepository {
        val inserted = mutableListOf<ActivitySessionEntity>()

        override fun getAllSessions(): Flow<List<ActivitySessionEntity>> = flowOf()
        override fun getActiveSessions(): Flow<List<ActivitySessionEntity>> = flowOf()
        override fun getSessionsForActivity(activityId: String): Flow<List<ActivitySessionEntity>> = flowOf()
        override fun getSessionsInRange(start: Instant, end: Instant): Flow<List<ActivitySessionEntity>> = flowOf()
        override suspend fun insertSession(session: ActivitySessionEntity) { inserted.add(session) }
        override suspend fun updateSession(session: ActivitySessionEntity) {}
        override suspend fun deleteSession(session: ActivitySessionEntity) {}
    }

    class FakeSyncMetadataRepository : SyncMetadataRepository {
        val inserted = mutableListOf<SyncMetadataEntity>()

        override suspend fun insertMetadata(metadata: SyncMetadataEntity) { inserted.add(metadata) }
        override suspend fun updateMetadata(metadata: SyncMetadataEntity) {}
        override suspend fun getPendingSyncs() = emptyList<SyncMetadataEntity>()
    }

    class FakeTransactionProvider : TransactionProvider {
        override suspend fun <T> runAsTransaction(block: suspend () -> T): T = block()
    }

    @Test
    fun testCreateSession_success() = runBlocking {
        val sessionRepo = FakeSessionRepository()
        val syncRepo = FakeSyncMetadataRepository()
        val transactionProvider = FakeTransactionProvider()
        val useCase = CreateManualSessionUseCase(sessionRepo, syncRepo, transactionProvider)

        val start = Instant.now()
        val end = start.plusSeconds(3600) // +1 hour

        useCase.invoke("Act1", start, end, "Test Note")

        assertEquals(1, sessionRepo.inserted.size)
        val createdSession = sessionRepo.inserted[0]
        assertEquals("Act1", createdSession.activityId)
        assertEquals(start, createdSession.startedAt)
        assertEquals(end, createdSession.endedAt)
        assertEquals("Test Note", createdSession.note)

        assertEquals(1, syncRepo.inserted.size)
        val syncMetadata = syncRepo.inserted[0]
        assertEquals(createdSession.id, syncMetadata.entityId)
        assertEquals("SESSION", syncMetadata.entityType)
        assertEquals("INSERT", syncMetadata.operation)
    }

    @Test
    fun testCreateSession_endTimeBeforeStartTime_throwsException() = runBlocking {
        val sessionRepo = FakeSessionRepository()
        val syncRepo = FakeSyncMetadataRepository()
        val transactionProvider = FakeTransactionProvider()
        val useCase = CreateManualSessionUseCase(sessionRepo, syncRepo, transactionProvider)

        val start = Instant.now()
        val end = start.minusSeconds(3600) // -1 hour, invalid

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCase.invoke("Act1", start, end, "Test Note")
            }
        }
        
        assertEquals("End time must be after or equal to start time", exception.message)
        assertEquals(0, sessionRepo.inserted.size)
        assertEquals(0, syncRepo.inserted.size)
    }
}
