package com.example.logapp.feature.tracking

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.StartActivityUseCase
import com.example.logapp.domain.usecase.StopActivityUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val startActivityUseCase = mockk<StartActivityUseCase>(relaxed = true)
    private val stopActivityUseCase = mockk<StopActivityUseCase>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val now = Instant.now()
    private val activities = listOf(
        ActivityEntity(id = "1", name = "Lập trình", sortOrder = 1, isArchived = false, createdAt = now, updatedAt = now),
        ActivityEntity(id = "2", name = "Đọc sách", sortOrder = 2, isArchived = false, createdAt = now, updatedAt = now),
        ActivityEntity(id = "3", name = "Hoạt động cũ", sortOrder = 3, isArchived = true, createdAt = now, updatedAt = now)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { activityRepository.getAllActivities() } returns flowOf(activities)
        every { sessionRepository.getActiveSessions() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUnarchivedActivities_filtersArchivedAndSorts() = runTest {
        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        backgroundScope.launch { viewModel.unarchivedActivities.collect {} }
        advanceUntilIdle()

        val result = viewModel.unarchivedActivities.value
        assertEquals(2, result.size)
        assertEquals("Lập trình", result[0].name)
        assertEquals("Đọc sách", result[1].name)
    }

    @Test
    fun testSearchFiltering_filtersActivitiesCorrectly() = runTest {
        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        backgroundScope.launch { viewModel.filteredActivities.collect {} }
        advanceUntilIdle()

        // When search query is empty
        assertEquals(2, viewModel.filteredActivities.value.size)

        // When search query matches "lập"
        viewModel.onSearchQueryChange("lập")
        advanceUntilIdle()
        assertEquals(1, viewModel.filteredActivities.value.size)
        assertEquals("Lập trình", viewModel.filteredActivities.value[0].name)

        // When search query matches nothing
        viewModel.onSearchQueryChange("bơi lội")
        advanceUntilIdle()
        assertEquals(0, viewModel.filteredActivities.value.size)
    }

    @Test
    fun testStartActivity_callsUseCase() = runTest {
        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        viewModel.startActivity("1")
        advanceUntilIdle()

        coVerify { startActivityUseCase("1") }
    }

    @Test
    fun testStopSession_callsUseCase() = runTest {
        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        val session = ActivitySessionEntity(
            id = "sess_1",
            activityId = "1",
            startedAt = now,
            createdAt = now,
            updatedAt = now
        )

        viewModel.stopSession(session)
        advanceUntilIdle()

        coVerify { stopActivityUseCase(session) }
    }

    @Test
    fun testUndoStopSession_restoresNullEndedAt() = runTest {
        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        val stoppedSession = ActivitySessionEntity(
            id = "sess_1",
            activityId = "1",
            startedAt = now.minusSeconds(3600),
            endedAt = now,
            createdAt = now,
            updatedAt = now
        )

        val slot = slot<ActivitySessionEntity>()
        coEvery { sessionRepository.updateSession(capture(slot)) } returns Unit

        viewModel.undoStopSession(stoppedSession)
        advanceUntilIdle()

        coVerify { sessionRepository.updateSession(any()) }
        assertNull("Restored session should have null endedAt", slot.captured.endedAt)
        assertEquals("sess_1", slot.captured.id)
    }

    @Test
    fun testToggleActivity_whenNotRunning_startsActivity() = runTest {
        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        backgroundScope.launch { viewModel.activeSessions.collect {} }
        advanceUntilIdle()

        viewModel.toggleActivity("1")
        advanceUntilIdle()

        coVerify { startActivityUseCase("1") }
    }

    @Test
    fun testToggleActivity_whenRunning_stopsSession() = runTest {
        val runningSession = ActivitySessionEntity(
            id = "sess_active_1",
            activityId = "1",
            startedAt = now,
            createdAt = now,
            updatedAt = now
        )
        every { sessionRepository.getActiveSessions() } returns flowOf(listOf(runningSession))

        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        backgroundScope.launch { viewModel.activeSessions.collect {} }
        advanceUntilIdle()

        viewModel.toggleActivity("1")
        advanceUntilIdle()

        coVerify { stopActivityUseCase(runningSession) }
    }

    @Test
    fun testActivitiesPagingChunking_preservesManagementSortOrder() = runTest {
        val manyActivities = (1..18).map { i ->
            ActivityEntity(
                id = "act_$i",
                name = "Hoạt động $i",
                sortOrder = i,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
        }
        every { activityRepository.getAllActivities() } returns flowOf(manyActivities)

        val viewModel = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase
        )
        backgroundScope.launch { viewModel.filteredActivities.collect {} }
        advanceUntilIdle()

        val pages = viewModel.filteredActivities.value.chunked(8)
        assertEquals(3, pages.size)
        assertEquals(8, pages[0].size)
        assertEquals(8, pages[1].size)
        assertEquals(2, pages[2].size)
        assertEquals("Hoạt động 1", pages[0][0].name)
        assertEquals("Hoạt động 8", pages[0][7].name)
        assertEquals("Hoạt động 9", pages[1][0].name)
        assertEquals("Hoạt động 17", pages[2][0].name)
    }
}
