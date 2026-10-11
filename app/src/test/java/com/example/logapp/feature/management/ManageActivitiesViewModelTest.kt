package com.example.logapp.feature.management

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.DeleteActivityUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ManageActivitiesViewModelTest {

    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val deleteActivityUseCase = mockk<DeleteActivityUseCase>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val now = Instant.now()
    private val sampleActivities = listOf(
        ActivityEntity(id = "1", name = "Lập trình", icon = "💻", sortOrder = 0, isArchived = false, createdAt = now, updatedAt = now),
        ActivityEntity(id = "2", name = "Đọc sách", icon = "📚", sortOrder = 1, isArchived = false, createdAt = now, updatedAt = now),
        ActivityEntity(id = "3", name = "Chơi game", icon = "🎮", sortOrder = 2, isArchived = true, createdAt = now, updatedAt = now)
    )

    private lateinit var viewModel: ManageActivitiesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { activityRepository.getAllActivities() } returns flowOf(sampleActivities)
        viewModel = ManageActivitiesViewModel(activityRepository, sessionRepository, deleteActivityUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testActiveAndArchivedActivities_separatedCorrectly() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeActivities.collect {}
            viewModel.archivedActivities.collect {}
        }

        advanceUntilIdle()

        assertEquals(2, viewModel.activeActivities.value.size)
        assertEquals("1", viewModel.activeActivities.value[0].id)
        assertEquals("2", viewModel.activeActivities.value[1].id)

        assertEquals(1, viewModel.archivedActivities.value.size)
        assertEquals("3", viewModel.archivedActivities.value[0].id)

        assertEquals(2, viewModel.activeCount.value)
        assertEquals(1, viewModel.archivedCount.value)

        collectJob.cancel()
    }

    @Test
    fun testSearchQuery_filtersActiveAndArchived() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeActivities.collect {}
            viewModel.archivedActivities.collect {}
        }

        advanceUntilIdle()

        viewModel.onSearchQueryChange("lập")
        advanceUntilIdle()

        assertEquals(1, viewModel.activeActivities.value.size)
        assertEquals("Lập trình", viewModel.activeActivities.value[0].name)
        assertEquals(0, viewModel.archivedActivities.value.size)

        viewModel.onSearchQueryChange("game")
        advanceUntilIdle()

        assertEquals(0, viewModel.activeActivities.value.size)
        assertEquals(1, viewModel.archivedActivities.value.size)
        assertEquals("Chơi game", viewModel.archivedActivities.value[0].name)

        collectJob.cancel()
    }

    @Test
    fun testValidateActivityName_detectsBlankAndDuplicate() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.rawActivities.collect {}
        }
        advanceUntilIdle()

        // Blank name
        val blankErr = viewModel.validateActivityName(null, "   ")
        assertNotNull(blankErr)
        assertTrue(blankErr!!.contains("không được để trống"))

        // Duplicate name
        val dupErr = viewModel.validateActivityName(null, "lập trình")
        assertNotNull(dupErr)
        assertTrue(dupErr!!.contains("đã tồn tại"))

        // Same activity name editing is allowed
        val selfEdit = viewModel.validateActivityName("1", "Lập trình")
        assertNull(selfEdit)

        // Valid new name
        val validErr = viewModel.validateActivityName(null, "Thể dục")
        assertNull(validErr)

        collectJob.cancel()
    }

    @Test
    fun testSaveActivity_createsNew() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.rawActivities.collect {}
        }
        advanceUntilIdle()

        val saved = viewModel.saveActivity(null, "Thể dục", 0xFF00FF00, "🏃")
        assertTrue(saved)

        advanceUntilIdle()

        val slot = slot<ActivityEntity>()
        coVerify { activityRepository.addActivity(capture(slot)) }
        assertEquals("Thể dục", slot.captured.name)
        assertEquals("🏃", slot.captured.icon)
        assertEquals(3, slot.captured.sortOrder) // max was 2, so 2 + 1 = 3

        collectJob.cancel()
    }

    @Test
    fun testSaveActivity_updatesExisting() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.rawActivities.collect {}
        }
        advanceUntilIdle()

        val saved = viewModel.saveActivity("1", "Lập trình Kotlin", 0xFF0000FF, "💻")
        assertTrue(saved)

        advanceUntilIdle()

        val slot = slot<ActivityEntity>()
        coVerify { activityRepository.updateActivity(capture(slot)) }
        assertEquals("1", slot.captured.id)
        assertEquals("Lập trình Kotlin", slot.captured.name)

        collectJob.cancel()
    }

    @Test
    fun testArchiveAndUnarchiveActivity() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.rawActivities.collect {}
        }
        advanceUntilIdle()

        viewModel.archiveActivity("1", archive = true)
        advanceUntilIdle()

        val slot1 = slot<ActivityEntity>()
        coVerify { activityRepository.updateActivity(capture(slot1)) }
        assertTrue(slot1.captured.isArchived)

        viewModel.archiveActivity("3", archive = false)
        advanceUntilIdle()

        val slot2 = slot<ActivityEntity>()
        coVerify { activityRepository.updateActivity(capture(slot2)) }
        assertFalse(slot2.captured.isArchived)

        collectJob.cancel()
    }

    @Test
    fun testUpdateSortOrder_updatesModifiedItems() = runTest {
        val reordered = listOf(
            sampleActivities[1], // "2" at index 0 (was sortOrder = 1)
            sampleActivities[0]  // "1" at index 1 (was sortOrder = 0)
        )

        viewModel.updateSortOrder(reordered)
        advanceUntilIdle()

        coVerify { activityRepository.updateActivity(match { it.id == "2" && it.sortOrder == 0 }) }
        coVerify { activityRepository.updateActivity(match { it.id == "1" && it.sortOrder == 1 }) }
    }

    @Test
    fun testDeleteActivity_blocksWhenHasSessions() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.rawActivities.collect {}
        }
        advanceUntilIdle()

        val dummySession = ActivitySessionEntity(
            id = "s1",
            activityId = "1",
            startedAt = now,
            endedAt = now.plusSeconds(3600),
            createdAt = now,
            updatedAt = now
        )
        every { sessionRepository.getSessionsForActivity("1") } returns flowOf(listOf(dummySession))

        var blockedCalled = false
        var successCalled = false

        viewModel.deleteActivity(
            id = "1",
            onHasSessions = { count, _ ->
                blockedCalled = true
                assertEquals(1, count)
            },
            onSuccess = { successCalled = true }
        )

        advanceUntilIdle()

        assertTrue(blockedCalled)
        assertFalse(successCalled)
        coVerify(exactly = 0) { deleteActivityUseCase(any()) }

        collectJob.cancel()
    }

    @Test
    fun testDeleteActivity_succeedsWhenZeroSessions() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.rawActivities.collect {}
        }
        advanceUntilIdle()

        every { sessionRepository.getSessionsForActivity("2") } returns flowOf(emptyList())
        coEvery { deleteActivityUseCase(any()) } returns Result.success(Unit)

        var blockedCalled = false
        var successCalled = false

        viewModel.deleteActivity(
            id = "2",
            onHasSessions = { _, _ -> blockedCalled = true },
            onSuccess = { successCalled = true }
        )

        advanceUntilIdle()

        assertFalse(blockedCalled)
        assertTrue(successCalled)
        coVerify(exactly = 1) { deleteActivityUseCase(match { it.id == "2" }) }

        collectJob.cancel()
    }
}
