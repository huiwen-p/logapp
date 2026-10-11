package com.example.logapp.feature

import com.example.logapp.core.network.NetworkMonitor
import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.engine.AnalyticsEngine
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.*
import com.example.logapp.feature.analytics.AnalyticsViewModel
import com.example.logapp.feature.management.ManageActivitiesViewModel
import com.example.logapp.feature.timeline.TimelineViewModel
import com.example.logapp.feature.tracking.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StatefulUiViewModelTest {

    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val startActivityUseCase = mockk<StartActivityUseCase>(relaxed = true)
    private val stopActivityUseCase = mockk<StopActivityUseCase>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val createManualSessionUseCase = mockk<CreateManualSessionUseCase>(relaxed = true)
    private val editSessionUseCase = mockk<EditSessionUseCase>(relaxed = true)
    private val deleteSessionUseCase = mockk<DeleteSessionUseCase>(relaxed = true)
    private val splitSessionUseCase = mockk<SplitSessionUseCase>(relaxed = true)
    private val mergeSessionUseCase = mockk<MergeSessionUseCase>(relaxed = true)
    private val deleteActivityUseCase = mockk<DeleteActivityUseCase>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private val now = Instant.now()
    private val sampleActivities = listOf(
        ActivityEntity(id = "1", name = "Lập trình", sortOrder = 0, isArchived = false, createdAt = now, updatedAt = now),
        ActivityEntity(id = "2", name = "Đọc sách", sortOrder = 1, isArchived = false, createdAt = now, updatedAt = now)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { activityRepository.getAllActivities() } returns flowOf(sampleActivities)
        every { sessionRepository.getAllSessions() } returns flowOf(emptyList())
        every { sessionRepository.getActiveSessions() } returns flowOf(emptyList())
        every { networkMonitor.isOnline } returns flowOf(true)
        every { preferencesManager.dayBoundary } returns flowOf("00:00")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testHomeViewModel_statefulLoadingAndNetwork() = runTest {
        val vm = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase,
            networkMonitor
        )

        backgroundScope.launch { vm.isLoading.collect {} }
        backgroundScope.launch { vm.isOnline.collect {} }
        backgroundScope.launch { vm.unarchivedActivities.collect {} }

        advanceUntilIdle()

        assertFalse(vm.isLoading.value)
        assertTrue(vm.isOnline.value)
        assertEquals(2, vm.unarchivedActivities.value.size)
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun testHomeViewModel_errorStateHandling() = runTest {
        every { activityRepository.getAllActivities() } returns flow {
            throw RuntimeException("Database error")
        }

        val vm = HomeViewModel(
            activityRepository,
            sessionRepository,
            startActivityUseCase,
            stopActivityUseCase,
            networkMonitor
        )

        backgroundScope.launch { vm.isLoading.collect {} }
        backgroundScope.launch { vm.errorMessage.collect {} }

        advanceUntilIdle()

        assertFalse(vm.isLoading.value)
        assertNotNull(vm.errorMessage.value)
        assertTrue(vm.errorMessage.value!!.contains("Database error"))
    }

    @Test
    fun testTimelineViewModel_statefulLoadingAndJumpToToday() = runTest {
        val vm = TimelineViewModel(
            sessionRepository,
            activityRepository,
            createManualSessionUseCase,
            editSessionUseCase,
            deleteSessionUseCase,
            splitSessionUseCase,
            mergeSessionUseCase,
            preferencesManager
        )

        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(LocalDate.now(), vm.uiState.value.selectedDate)

        vm.changeDate(-3)
        advanceUntilIdle()
        assertEquals(LocalDate.now().minusDays(3), vm.uiState.value.selectedDate)

        vm.jumpToToday()
        advanceUntilIdle()
        assertEquals(LocalDate.now(), vm.uiState.value.selectedDate)
    }

    @Test
    fun testAnalyticsViewModel_statefulCalculationAndTabSwitching() = runTest {
        val vm = AnalyticsViewModel(
            sessionRepository,
            activityRepository,
            preferencesManager
        )

        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
        assertNotNull(vm.uiState.value.dailyAnalytics)

        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun testManageActivitiesViewModel_loadingAndStatefulFlows() = runTest {
        val vm = ManageActivitiesViewModel(
            activityRepository,
            sessionRepository,
            deleteActivityUseCase
        )

        backgroundScope.launch { vm.isLoading.collect {} }
        backgroundScope.launch { vm.activeActivities.collect {} }
        backgroundScope.launch { vm.rawActivities.collect {} }

        advanceUntilIdle()

        assertFalse(vm.isLoading.value)
        assertNull(vm.errorMessage.value)
        assertEquals(2, vm.activeActivities.value.size)
    }
}
