package com.example.logapp.feature.analytics

import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
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
    fun testInitialUiState_defaultsToDailyAndToday() = runTest {
        val viewModel = AnalyticsViewModel(sessionRepository, activityRepository, preferencesManager)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnalyticsTab.DAILY, state.selectedTab)
        assertEquals(today, state.selectedDate)
        assertEquals(1, state.activities.size)
        assertNotNull(state.dailyAnalytics)
    }

    @Test
    fun testSetTab_switchesTabAndComputesAnalytics() = runTest {
        val viewModel = AnalyticsViewModel(sessionRepository, activityRepository, preferencesManager)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setTab(AnalyticsTab.WEEKLY)
        advanceUntilIdle()
        assertEquals(AnalyticsTab.WEEKLY, viewModel.uiState.value.selectedTab)
        assertNotNull(viewModel.uiState.value.weeklyAnalytics)

        viewModel.setTab(AnalyticsTab.MONTHLY)
        advanceUntilIdle()
        assertEquals(AnalyticsTab.MONTHLY, viewModel.uiState.value.selectedTab)
        assertNotNull(viewModel.uiState.value.monthlyAnalytics)
    }

    @Test
    fun testPeriodNavigation_dailyPeriod() = runTest {
        val viewModel = AnalyticsViewModel(sessionRepository, activityRepository, preferencesManager)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.previousPeriod()
        advanceUntilIdle()
        assertEquals(today.minusDays(1), viewModel.uiState.value.selectedDate)

        viewModel.nextPeriod()
        advanceUntilIdle()
        assertEquals(today, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testPeriodNavigation_weeklyPeriod() = runTest {
        val viewModel = AnalyticsViewModel(sessionRepository, activityRepository, preferencesManager)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setTab(AnalyticsTab.WEEKLY)
        advanceUntilIdle()

        viewModel.previousPeriod()
        advanceUntilIdle()
        assertEquals(today.minusWeeks(1), viewModel.uiState.value.selectedDate)

        viewModel.nextPeriod()
        advanceUntilIdle()
        assertEquals(today, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testPeriodNavigation_monthlyPeriod() = runTest {
        val viewModel = AnalyticsViewModel(sessionRepository, activityRepository, preferencesManager)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setTab(AnalyticsTab.MONTHLY)
        advanceUntilIdle()

        viewModel.previousPeriod()
        advanceUntilIdle()
        assertEquals(today.minusMonths(1), viewModel.uiState.value.selectedDate)

        viewModel.nextPeriod()
        advanceUntilIdle()
        assertEquals(today, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testResetToCurrentPeriod() = runTest {
        val viewModel = AnalyticsViewModel(sessionRepository, activityRepository, preferencesManager)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setDate(today.minusDays(10))
        advanceUntilIdle()
        assertEquals(today.minusDays(10), viewModel.uiState.value.selectedDate)

        viewModel.resetToCurrentPeriod()
        advanceUntilIdle()
        assertEquals(today, viewModel.uiState.value.selectedDate)
    }
}
