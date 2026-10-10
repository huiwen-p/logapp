package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.exception.ActivityHasSessionsException
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DeleteActivityUseCaseTest {

    class FakeActivityRepository : ActivityRepository {
        val deleted = mutableListOf<ActivityEntity>()

        override fun getActiveActivities(): Flow<List<ActivityEntity>> = flowOf()
        override fun getAllActivities(): Flow<List<ActivityEntity>> = flowOf()
        override suspend fun getActivityById(id: String): ActivityEntity? = null
        override suspend fun addActivity(activity: ActivityEntity) {}
        override suspend fun updateActivity(activity: ActivityEntity) {}
        override suspend fun deleteActivity(activity: ActivityEntity) { deleted.add(activity) }
    }

    class FakeSessionRepository(private val sessions: List<ActivitySessionEntity> = emptyList()) : SessionRepository {
        override fun getAllSessions(): Flow<List<ActivitySessionEntity>> = flowOf(sessions)
        override fun getActiveSessions(): Flow<List<ActivitySessionEntity>> = flowOf(sessions)
        override fun getSessionsForActivity(activityId: String): Flow<List<ActivitySessionEntity>> = flowOf(sessions.filter { it.activityId == activityId })
        override fun getSessionsInRange(start: Instant, end: Instant): Flow<List<ActivitySessionEntity>> = flowOf()
        override suspend fun insertSession(session: ActivitySessionEntity) {}
        override suspend fun updateSession(session: ActivitySessionEntity) {}
        override suspend fun deleteSession(session: ActivitySessionEntity) {}
    }

    @Test
    fun testDeleteActivity_withSessions_throwsException() = runBlocking {
        val activityRepo = FakeActivityRepository()
        val session = ActivitySessionEntity(id = "S1", activityId = "Act1", startedAt = Instant.now(), createdAt = Instant.now(), updatedAt = Instant.now())
        val sessionRepo = FakeSessionRepository(listOf(session))
        val useCase = DeleteActivityUseCase(activityRepo, sessionRepo)

        val activity = ActivityEntity(id = "Act1", name = "Test", createdAt = Instant.now(), updatedAt = Instant.now())

        val result = useCase(activity)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ActivityHasSessionsException)
        assertEquals(0, activityRepo.deleted.size)
    }

    @Test
    fun testDeleteActivity_withoutSessions_success() = runBlocking {
        val activityRepo = FakeActivityRepository()
        val sessionRepo = FakeSessionRepository(emptyList())
        val useCase = DeleteActivityUseCase(activityRepo, sessionRepo)

        val activity = ActivityEntity(id = "Act1", name = "Test", createdAt = Instant.now(), updatedAt = Instant.now())

        val result = useCase(activity)

        assertTrue(result.isSuccess)
        assertEquals(1, activityRepo.deleted.size)
    }
}
