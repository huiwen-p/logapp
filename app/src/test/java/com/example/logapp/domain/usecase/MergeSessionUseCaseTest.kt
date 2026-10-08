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

class MergeSessionUseCaseTest {

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
        override suspend fun <T> runAsTransaction(block: suspend () -> T): T = block()
    }

    @Test
    fun testMergeSession() = runBlocking {
        val sessionRepo = FakeSessionRepository()
        val syncRepo = FakeSyncMetadataRepository()
        val transactionProvider = FakeTransactionProvider()
        val useCase = MergeSessionUseCase(sessionRepo, syncRepo, transactionProvider)

        val s1 = ActivitySessionEntity(
            id = "S1",
            activityId = "Act1",
            startedAt = Instant.parse("2026-09-30T08:00:00Z"),
            endedAt = Instant.parse("2026-09-30T09:00:00Z"),
            createdAt = Instant.now(), updatedAt = Instant.now(), note = "Note 1", version = 1
        )
        val s2 = ActivitySessionEntity(
            id = "S2",
            activityId = "Act1",
            startedAt = Instant.parse("2026-09-30T08:30:00Z"),
            endedAt = Instant.parse("2026-09-30T10:00:00Z"),
            createdAt = Instant.now(), updatedAt = Instant.now(), note = "Note 2", version = 1
        )

        useCase.invoke(s1, s2)

        // 2 soft-deleted (update)
        assertEquals(2, sessionRepo.updated.size)
        assertTrue(sessionRepo.updated.any { it.id == "S1" && it.deletedAt != null })
        assertTrue(sessionRepo.updated.any { it.id == "S2" && it.deletedAt != null })
        
        // 1 inserted (merged session)
        assertEquals(1, sessionRepo.inserted.size)
        val merged = sessionRepo.inserted[0]
        assertEquals(Instant.parse("2026-09-30T08:00:00Z"), merged.startedAt)
        assertEquals(Instant.parse("2026-09-30T10:00:00Z"), merged.endedAt)
        assertEquals("Note 1 | Note 2", merged.note)
        
        // 3 sync metadata (2 delete, 1 insert)
        assertEquals(3, syncRepo.inserted.size)
        assertEquals(2, syncRepo.inserted.count { it.operation == "DELETE" })
        assertEquals(1, syncRepo.inserted.count { it.operation == "INSERT" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMergeSession_differentActivity() = runBlocking {
        val sessionRepo = FakeSessionRepository()
        val syncRepo = FakeSyncMetadataRepository()
        val transactionProvider = FakeTransactionProvider()
        val useCase = MergeSessionUseCase(sessionRepo, syncRepo, transactionProvider)

        val s1 = ActivitySessionEntity(
            id = "S1", activityId = "Act1",
            startedAt = Instant.now(), createdAt = Instant.now(), updatedAt = Instant.now()
        )
        val s2 = ActivitySessionEntity(
            id = "S2", activityId = "Act2",
            startedAt = Instant.now(), createdAt = Instant.now(), updatedAt = Instant.now()
        )

        useCase.invoke(s1, s2)
    }
}
