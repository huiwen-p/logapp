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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DeleteSessionUseCaseTest {

    class FakeSessionRepository : SessionRepository {
        val updated = mutableListOf<ActivitySessionEntity>()

        override fun getAllSessions(): Flow<List<ActivitySessionEntity>> = flowOf()
        override fun getActiveSessions(): Flow<List<ActivitySessionEntity>> = flowOf()
        override fun getSessionsForActivity(activityId: String): Flow<List<ActivitySessionEntity>> = flowOf()
        override fun getSessionsInRange(start: Instant, end: Instant): Flow<List<ActivitySessionEntity>> = flowOf()
        override suspend fun insertSession(session: ActivitySessionEntity) {}
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
    fun testDeleteSession_softDeletesAndCreatesSyncMetadata() = runBlocking {
        val sessionRepo = FakeSessionRepository()
        val syncRepo = FakeSyncMetadataRepository()
        val transactionProvider = FakeTransactionProvider()
        val useCase = DeleteSessionUseCase(sessionRepo, syncRepo, transactionProvider)

        val session = ActivitySessionEntity(
            id = "S1",
            activityId = "Act1",
            startedAt = Instant.now(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 1
        )

        useCase.invoke(session)

        // Verify soft delete
        assertEquals(1, sessionRepo.updated.size)
        val updatedSession = sessionRepo.updated[0]
        assertNotNull(updatedSession.deletedAt)
        assertEquals(2, updatedSession.version)

        // Verify sync metadata
        assertEquals(1, syncRepo.inserted.size)
        val syncMetadata = syncRepo.inserted[0]
        assertEquals("S1", syncMetadata.entityId)
        assertEquals("SESSION", syncMetadata.entityType)
        assertEquals("DELETE", syncMetadata.operation)
    }
}
