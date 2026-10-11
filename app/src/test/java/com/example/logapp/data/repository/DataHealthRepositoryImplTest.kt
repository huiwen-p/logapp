package com.example.logapp.data.repository

import com.example.logapp.data.local.dao.DataHealthDao
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DataHealthRepositoryImplTest {

    private lateinit var dataHealthDao: DataHealthDao
    private lateinit var repository: DataHealthRepositoryImpl

    @Before
    fun setUp() {
        dataHealthDao = mockk(relaxed = true)
        repository = DataHealthRepositoryImpl(dataHealthDao)
    }

    @Test
    fun `checkHealth returns correct healthy report when no issues exist`() = runTest {
        coEvery { dataHealthDao.getTotalActivitiesCount() } returns 5
        coEvery { dataHealthDao.getTotalSessionsCount() } returns 20
        coEvery { dataHealthDao.getOrphanSessions() } returns emptyList()
        coEvery { dataHealthDao.getInvalidTimestampSessions() } returns emptyList()

        val report = repository.checkHealth()

        assertEquals(5, report.totalActivities)
        assertEquals(20, report.totalSessions)
        assertEquals(0, report.orphanSessionsCount)
        assertEquals(0, report.invalidTimestampCount)
        assertTrue(report.isHealthy)
        assertFalse(report.hasIssues)
    }

    @Test
    fun `checkHealth returns unhealthy report when orphan and timestamp issues exist`() = runTest {
        val now = Instant.now()
        val orphanSession = ActivitySessionEntity(
            id = "orphan-1",
            activityId = "non-existent",
            startedAt = now,
            createdAt = now,
            updatedAt = now
        )
        val invalidSession = ActivitySessionEntity(
            id = "invalid-1",
            activityId = "act-1",
            startedAt = now.plusSeconds(3600),
            endedAt = now, // endedAt before startedAt!
            createdAt = now,
            updatedAt = now
        )

        coEvery { dataHealthDao.getTotalActivitiesCount() } returns 3
        coEvery { dataHealthDao.getTotalSessionsCount() } returns 10
        coEvery { dataHealthDao.getOrphanSessions() } returns listOf(orphanSession)
        coEvery { dataHealthDao.getInvalidTimestampSessions() } returns listOf(invalidSession)

        val report = repository.checkHealth()

        assertEquals(1, report.orphanSessionsCount)
        assertEquals(1, report.invalidTimestampCount)
        assertFalse(report.isHealthy)
        assertTrue(report.hasIssues)
    }

    @Test
    fun `repairHealth fixes orphan sessions and invalid timestamps`() = runTest {
        val now = Instant.now()
        val orphanSession = ActivitySessionEntity(
            id = "orphan-1",
            activityId = "non-existent",
            startedAt = now,
            createdAt = now,
            updatedAt = now
        )
        val invalidSession = ActivitySessionEntity(
            id = "invalid-1",
            activityId = "act-1",
            startedAt = now.plusSeconds(3600),
            endedAt = now,
            createdAt = now,
            updatedAt = now
        )

        coEvery { dataHealthDao.getOrphanSessions() } returns listOf(orphanSession) andThen emptyList()
        coEvery { dataHealthDao.getInvalidTimestampSessions() } returns listOf(invalidSession) andThen emptyList()
        coEvery { dataHealthDao.getActivityByName("Chưa phân loại") } returns null
        coEvery { dataHealthDao.insertActivity(any()) } just Runs
        coEvery { dataHealthDao.updateSessions(any()) } just Runs
        coEvery { dataHealthDao.getTotalActivitiesCount() } returns 4
        coEvery { dataHealthDao.getTotalSessionsCount() } returns 10

        val report = repository.repairHealth()

        coVerify { dataHealthDao.insertActivity(match { it.name == "Chưa phân loại" }) }
        coVerify {
            dataHealthDao.updateSessions(match { sessions ->
                val orphan = sessions.find { it.id == "orphan-1" }
                val invalid = sessions.find { it.id == "invalid-1" }
                orphan != null && invalid != null &&
                        invalid.startedAt.isBefore(invalid.endedAt!!)
            })
        }
        assertTrue(report.isHealthy)
    }
}
