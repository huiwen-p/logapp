package com.example.logapp.domain.usecase

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ArchiveActivityUseCaseTest {

    class FakeActivityRepository : ActivityRepository {
        val updated = mutableListOf<ActivityEntity>()

        override fun getActiveActivities(): Flow<List<ActivityEntity>> = flowOf()
        override fun getAllActivities(): Flow<List<ActivityEntity>> = flowOf()
        override suspend fun getActivityById(id: String): ActivityEntity? = null
        override suspend fun addActivity(activity: ActivityEntity) {}
        override suspend fun updateActivity(activity: ActivityEntity) { updated.add(activity) }
        override suspend fun deleteActivity(activity: ActivityEntity) {}
    }

    @Test
    fun testArchiveActivity() = runBlocking {
        val repository = FakeActivityRepository()
        val useCase = ArchiveActivityUseCase(repository)

        val activity = ActivityEntity(
            id = "Act1",
            name = "Test",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )

        val result = useCase(activity)

        assertTrue(result.isSuccess)
        assertEquals(1, repository.updated.size)
        assertTrue(repository.updated[0].isArchived)
    }
}
