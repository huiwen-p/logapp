package com.example.logapp.feature.timeline

import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.*
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val createManualSessionUseCase = mockk<CreateManualSessionUseCase>(relaxed = true)
    private val editSessionUseCase = mockk<EditSessionUseCase>(relaxed = true)
    private val deleteSessionUseCase = mockk<DeleteSessionUseCase>(relaxed = true)
    private val splitSessionUseCase = mockk<SplitSessionUseCase>(relaxed = true)
    private val mergeSessionUseCase = mockk<MergeSessionUseCase>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val today = LocalDate.now()
    private val now = Instant.now()
    private val mockActivities = listOf(
        ActivityEntity(id = "act1", name = "Lập trình", sortOrder = 1, isArchived = false, createdAt = now, updatedAt = now)
    )

    private val mockSessions = listOf(
        ActivitySessionEntity(
            id = "sess1",
            activityId = "act1",
            startedAt = now.minusSeconds(3600),
            endedAt = now,
            createdAt = now,
            updatedAt = now,
            version = 1
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { activityRepository.getAllActivities() } returns flowOf(mockActivities)
        every { sessionRepository.getAllSessions() } returns flowOf(mockSessions)
        every { preferencesManager.dayBoundary } returns flowOf("00:00")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState_loadsSessionsAndActivities() = runTest {
        val viewModel = TimelineViewModel(
            sessionRepository,
            activityRepository,
            createManualSessionUseCase,
            editSessionUseCase,
            deleteSessionUseCase,
            splitSessionUseCase,
            mergeSessionUseCase,
            preferencesManager
        )

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(today, state.selectedDate)
        assertEquals(1, state.activities.size)
        assertEquals("act1", state.activities[0].id)
        assertTrue(state.sessions.isNotEmpty())
    }

    @Test
    fun testChangeDate_updatesSelectedDate() = runTest {
        val viewModel = TimelineViewModel(
            sessionRepository,
            activityRepository,
            createManualSessionUseCase,
            editSessionUseCase,
            deleteSessionUseCase,
            splitSessionUseCase,
            mergeSessionUseCase,
            preferencesManager
        )

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.changeDate(1)
        advanceUntilIdle()
        assertEquals(today.plusDays(1), viewModel.uiState.value.selectedDate)

        viewModel.changeDate(-2)
        advanceUntilIdle()
        assertEquals(today.minusDays(1), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testSplitSession_invokesUseCase() = runTest {
        val viewModel = TimelineViewModel(
            sessionRepository,
            activityRepository,
            createManualSessionUseCase,
            editSessionUseCase,
            deleteSessionUseCase,
            splitSessionUseCase,
            mergeSessionUseCase,
            preferencesManager
        )

        val session = mockSessions[0]
        val splitAt = now.minusSeconds(1800)

        viewModel.splitSession(session, splitAt)
        advanceUntilIdle()

        coVerify(exactly = 1) { splitSessionUseCase(session, splitAt) }
    }

    @Test
    fun testMergeSessions_invokesUseCase() = runTest {
        val viewModel = TimelineViewModel(
            sessionRepository,
            activityRepository,
            createManualSessionUseCase,
            editSessionUseCase,
            deleteSessionUseCase,
            splitSessionUseCase,
            mergeSessionUseCase,
            preferencesManager
        )

        val s1 = mockSessions[0]
        val s2 = s1.copy(id = "sess2", startedAt = now, endedAt = now.plusSeconds(1800))

        viewModel.mergeSessions(s1, s2)
        advanceUntilIdle()

        coVerify(exactly = 1) { mergeSessionUseCase(s1, s2) }
    }

    @Test
    fun testDeleteSession_invokesUseCase() = runTest {
        val viewModel = TimelineViewModel(
            sessionRepository,
            activityRepository,
            createManualSessionUseCase,
            editSessionUseCase,
            deleteSessionUseCase,
            splitSessionUseCase,
            mergeSessionUseCase,
            preferencesManager
        )

        val session = mockSessions[0]
        viewModel.deleteSession(session)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteSessionUseCase(session) }
    }

    @Test
    fun testEditSession_invokesUseCase() = runTest {
        val viewModel = TimelineViewModel(
            sessionRepository,
            activityRepository,
            createManualSessionUseCase,
            editSessionUseCase,
            deleteSessionUseCase,
            splitSessionUseCase,
            mergeSessionUseCase,
            preferencesManager
        )

        val session = mockSessions[0]
        val newStart = now.minusSeconds(7200)
        val newEnd = now.minusSeconds(100)
        val newNote = "Ghi chú đã sửa"

        viewModel.editSession(session, newStart, newEnd, newNote)
        advanceUntilIdle()

        coVerify(exactly = 1) { editSessionUseCase(session, newStart, newEnd, newNote) }
    }
}
