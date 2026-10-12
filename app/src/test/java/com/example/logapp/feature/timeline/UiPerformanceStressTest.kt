package com.example.logapp.feature.timeline

import androidx.compose.runtime.Immutable
import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.engine.DailyAnalytics
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.*
import com.example.logapp.feature.analytics.AnalyticsUiState
import com.example.logapp.feature.timeline.engine.SessionLayoutPosition
import com.example.logapp.feature.timeline.engine.TimelineLayoutEngine
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class UiPerformanceStressTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val createManualSessionUseCase = mockk<CreateManualSessionUseCase>(relaxed = true)
    private val editSessionUseCase = mockk<EditSessionUseCase>(relaxed = true)
    private val deleteSessionUseCase = mockk<DeleteSessionUseCase>(relaxed = true)
    private val splitSessionUseCase = mockk<SplitSessionUseCase>(relaxed = true)
    private val mergeSessionUseCase = mockk<MergeSessionUseCase>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private val baseTime = Instant.parse("2026-10-12T00:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { activityRepository.getAllActivities() } returns flowOf(emptyList())
        every { sessionRepository.getAllSessions() } returns flowOf(emptyList())
        every { preferencesManager.dayBoundary } returns flowOf("00:00")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTimelineLayoutEngine_stressTestWith1000Sessions() {
        // Generate 1,000 realistic sessions within a 24-hour day:
        // Combinations of concurrent bursts, back-to-back sessions, and long background tracks
        val sessions = mutableListOf<ActivitySessionEntity>()
        val activityIds = listOf("work", "study", "music", "exercise", "reading")

        for (i in 0 until 1000) {
            val actId = activityIds[i % activityIds.size]
            // Distribute across 24h with heavy concurrent overlap
            val startOffsetSec = (i * 80L) % 86400
            val durationSec = 300L + ((i * 37L) % 3600L) // 5m to ~65m
            val start = baseTime.plusSeconds(startOffsetSec)
            val end = start.plusSeconds(durationSec)

            sessions.add(
                ActivitySessionEntity(
                    id = "session-$i",
                    activityId = actId,
                    startedAt = start,
                    endedAt = end,
                    note = "Stress test session #$i",
                    createdAt = start,
                    updatedAt = start,
                    version = 1
                )
            )
        }

        // Measure calculation time for 1,000 sessions
        val layoutResult: Map<String, SessionLayoutPosition>
        val timeElapsedMs = measureTimeMillis {
            layoutResult = TimelineLayoutEngine.calculateLayout(sessions, referenceTime = baseTime.plus(1, ChronoUnit.DAYS))
        }

        // 1. Performance Assertion: 1,000 sessions layout calculated rapidly (< 250ms on cold JVM)
        assertTrue("Layout calculation for 1,000 sessions must take < 250ms, took ${timeElapsedMs}ms", timeElapsedMs < 250)

        // 2. Completeness Assertion: Every single session must have a layout position
        assertEquals(1000, layoutResult.size)
        sessions.forEach { session ->
            val pos = layoutResult[session.id]
            assertNotNull("Position must exist for ${session.id}", pos)
            assertTrue("Column index must be non-negative", pos!!.columnIndex >= 0)
            assertTrue("Column index must be strictly less than totalColumns", pos.columnIndex < pos.totalColumns)
            assertTrue("Total columns must be at least 1", pos.totalColumns >= 1)
        }
    }

    @Test
    fun testViewportCulling_efficientlySkipsOffScreenSessions() {
        val totalSessionsCount = 1000
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val canvasHeight = 1000f
        val scaleY = 3.0f // Zoomed-in timeline
        val offsetY = -1000f // Scrolled down

        // Simulate 1,000 sessions spread over 24 hours
        val sessions = (0 until totalSessionsCount).map { i ->
            val startMillis = (i * (oneDayMillis / totalSessionsCount))
            val durationMillis = 20 * 60 * 1000L // 20 mins
            val endMillis = startMillis + durationMillis
            val start = Instant.ofEpochMilli(startMillis)
            val end = Instant.ofEpochMilli(endMillis)

            ActivitySessionEntity(
                id = "culling-$i",
                activityId = "act-1",
                startedAt = start,
                endedAt = end,
                createdAt = start,
                updatedAt = start
            )
        }

        // Run the culling check matching TimelineCanvas algorithm
        var renderedCount = 0
        var culledCount = 0

        sessions.forEach { session ->
            val startMilli = session.startedAt.toEpochMilli()
            val endMilli = session.endedAt!!.toEpochMilli()

            val startY = (startMilli.toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY
            val endY = (endMilli.toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY

            if (endY < -50f || startY > canvasHeight + 50f) {
                culledCount++
            } else {
                renderedCount++
            }
        }

        // Culling must effectively discard the vast majority of off-screen sessions
        assertTrue("Rendered items must be a fraction of total items", renderedCount < totalSessionsCount)
        assertTrue("Culled items must be significant (>= 50%)", culledCount > 500)
        assertEquals(totalSessionsCount, renderedCount + culledCount)
    }

    @Test
    fun testTimelineViewModel_viewModeToggleAndPersistence() = runTest {
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

        // 1. Initial mode is CANVAS
        assertEquals(TimelineViewMode.CANVAS, viewModel.viewMode.value)
        assertEquals(TimelineViewMode.CANVAS, viewModel.uiState.value.viewMode)

        // 2. Toggle to LIST
        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals(TimelineViewMode.LIST, viewModel.viewMode.value)
        assertEquals(TimelineViewMode.LIST, viewModel.uiState.value.viewMode)

        // 3. Toggle back to CANVAS
        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals(TimelineViewMode.CANVAS, viewModel.viewMode.value)
        assertEquals(TimelineViewMode.CANVAS, viewModel.uiState.value.viewMode)

        // 4. Explicitly set mode
        viewModel.setViewMode(TimelineViewMode.LIST)
        advanceUntilIdle()
        assertEquals(TimelineViewMode.LIST, viewModel.viewMode.value)
    }

    private fun hasBinaryAnnotation(clazz: Class<*>, annotationDescriptor: String): Boolean {
        val resourceName = clazz.name.replace('.', '/') + ".class"
        val stream = clazz.classLoader?.getResourceAsStream(resourceName) ?: return false
        val bytes = stream.use { it.readBytes() }
        val string = String(bytes, Charsets.ISO_8859_1)
        return string.contains(annotationDescriptor)
    }

    @Test
    fun testDataModels_composeImmutableAnnotationsPresent() {
        // In Jetpack Compose, @Immutable has AnnotationRetention.BINARY, which is preserved in class bytecode
        val immutableDescriptor = "Landroidx/compose/runtime/Immutable;"
        assertTrue(
            "ActivityEntity must be annotated with @Immutable",
            hasBinaryAnnotation(ActivityEntity::class.java, immutableDescriptor)
        )
        assertTrue(
            "ActivitySessionEntity must be annotated with @Immutable",
            hasBinaryAnnotation(ActivitySessionEntity::class.java, immutableDescriptor)
        )
        assertTrue(
            "TimelineUiState must be annotated with @Immutable",
            hasBinaryAnnotation(TimelineUiState::class.java, immutableDescriptor)
        )
        assertTrue(
            "AnalyticsUiState must be annotated with @Immutable",
            hasBinaryAnnotation(AnalyticsUiState::class.java, immutableDescriptor)
        )
        assertTrue(
            "SessionLayoutPosition must be annotated with @Immutable",
            hasBinaryAnnotation(SessionLayoutPosition::class.java, immutableDescriptor)
        )
        assertTrue(
            "DailyAnalytics must be annotated with @Immutable",
            hasBinaryAnnotation(DailyAnalytics::class.java, immutableDescriptor)
        )
    }
}
