package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.repository.TransactionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SplitSessionUseCaseTest {

    class FakeSessionRepository : SessionRepository {
        val inserted = mutableListOf<ActivitySessionEntity>()
        val updated = mutableListOf<ActivitySessionEntity>()
        
        override fun getAllSessions() = flowOf<List<ActivitySessionEntity>>()
        override fun getActiveSessions() = flowOf<List<ActivitySessionEntity>>()
        override fun getSessionsForActivity(activityId: String) = flowOf<List<ActivitySessionEntity>>()
        override fun getSessionsInRange(start: Instant, end: Instant) = flowOf<List<ActivitySessionEntity>>()
        
        override suspend fun insertSession(session: ActivitySessionEntity) { inserted.add(session) }
        override suspend fun updateSession(session: ActivitySessionEntity) { updated.add(session) }
        override suspend fun deleteSession(session: ActivitySessionEntity) {}
    }

    class FakeSyncMetadataRepository : SyncMetadataRepository {
        val inserted = mutableListOf<SyncMetadataEntity>()
        
        override suspend fun insertMetadata(metadata: SyncMetadataEntity) { inserted.add(metadata) }
        override suspend fun updateMetadata(metadata: SyncMetadataEntity) {}
        override suspend fun getPendingSyncs() = emptyList<SyncMetadataEntity>()
    }

    class FakeTransactionProvider : TransactionProvider {
        var transactionCalled = false
        override suspend fun <T> runAsTransaction(block: suspend () -> T): T {
            transactionCalled = true
            return block()
        }
    }

    @Test
    fun testSplitSession() = runBlocking {
        val sessionRepo = FakeSessionRepository()
        val syncRepo = FakeSyncMetadataRepository()
        val transactionProvider = FakeTransactionProvider()
        val useCase = SplitSessionUseCase(sessionRepo, syncRepo, transactionProvider)

        val startTime = Instant.parse("2026-09-30T08:00:00Z")
        val endTime = Instant.parse("2026-09-30T10:00:00Z")
        val splitTime = Instant.parse("2026-09-30T09:00:00Z")

        val originalSession = ActivitySessionEntity(
            id = "ORIGINAL_ID",
            activityId = "Act1",
            startedAt = startTime,
            endedAt = endTime,
            createdAt = startTime,
            updatedAt = startTime,
            version = 1
        )

        useCase.invoke(originalSession, splitTime)

        // Verifications
        assertTrue(transactionProvider.transactionCalled)
        
        // 1 soft-deleted (update)
        assertEquals(1, sessionRepo.updated.size)
        assertNotNull(sessionRepo.updated[0].deletedAt)
        assertEquals("ORIGINAL_ID", sessionRepo.updated[0].id)
        
        // 2 inserted (new sessions)
        assertEquals(2, sessionRepo.inserted.size)
        val s1 = sessionRepo.inserted.find { it.startedAt == startTime }!!
        val s2 = sessionRepo.inserted.find { it.startedAt == splitTime }!!
        
        assertEquals(splitTime, s1.endedAt)
        assertEquals(endTime, s2.endedAt)
        
        // 3 sync metadata (1 delete, 2 insert)
        assertEquals(3, syncRepo.inserted.size)
        val deleteMeta = syncRepo.inserted.find { it.operation == "DELETE" }!!
        val insertMetas = syncRepo.inserted.filter { it.operation == "INSERT" }
        
        assertEquals("ORIGINAL_ID", deleteMeta.entityId)
        assertEquals(2, insertMetas.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSplitSession_invalidTime() = runBlocking {
        val sessionRepo = FakeSessionRepository()
        val syncRepo = FakeSyncMetadataRepository()
        val transactionProvider = FakeTransactionProvider()
        val useCase = SplitSessionUseCase(sessionRepo, syncRepo, transactionProvider)

        val startTime = Instant.parse("2026-09-30T08:00:00Z")
        val endTime = Instant.parse("2026-09-30T10:00:00Z")
        val splitTime = Instant.parse("2026-09-30T07:00:00Z") // Before start

        val originalSession = ActivitySessionEntity(
            id = "ORIGINAL_ID",
            activityId = "Act1",
            startedAt = startTime,
            endedAt = endTime,
            createdAt = startTime,
            updatedAt = startTime
        )

        useCase.invoke(originalSession, splitTime)
    }
}
