package com.example.logapp.domain.engine

import com.example.logapp.data.local.entity.ActivitySessionEntity
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class BackendPerformanceStressTest {

    @Test
    fun testStressTest_10000Sessions_OverlapEnginePerformance() {
        val count = 10_000
        val baseTime = Instant.parse("2026-09-01T00:00:00Z")
        val random = Random(42) // Fixed seed for reproducible benchmarks
        val activityIds = listOf("Act_Work", "Act_Study", "Act_Workout", "Act_Reading", "Act_Coding")

        val sessions = ArrayList<ActivitySessionEntity>(count)
        for (i in 0 until count) {
            // Offset randomly across 30 days (up to 43,200 minutes)
            val offsetMinutes = random.nextLong(0, 43_200)
            val durationMinutes = random.nextLong(15, 180) // 15 to 180 minutes duration
            val startTime = baseTime.plus(offsetMinutes, ChronoUnit.MINUTES)
            val endTime = startTime.plus(durationMinutes, ChronoUnit.MINUTES)

            sessions.add(
                ActivitySessionEntity(
                    id = "session_$i",
                    activityId = activityIds[random.nextInt(activityIds.size)],
                    startedAt = startTime,
                    endedAt = endTime,
                    createdAt = startTime,
                    updatedAt = startTime
                )
            )
        }

        val currentTime = baseTime.plus(45, ChronoUnit.DAYS)

        // Warm-up JIT with small subset
        OverlapEngine.calculateSegments(sessions.take(100), currentTime)

        // Measure OverlapEngine with all 10,000 sessions
        val durationMillis = measureTimeMillis {
            val segments = OverlapEngine.calculateSegments(sessions, currentTime)
            assertTrue("Segments should not be empty", segments.isNotEmpty())
        }

        println("=== Performance Benchmark ===")
        println("Processed $count sessions with OverlapEngine in: ${durationMillis}ms")

        // 10,000 sessions should execute under 2000ms with O(N log N) sweep-line algorithm
        assertTrue(
            "Expected OverlapEngine for 10k sessions to complete within 2000ms, took ${durationMillis}ms",
            durationMillis < 2000
        )
    }

    @Test
    fun testStressTest_HeavyDayAnalytics_Performance() {
        val count = 1_000 // 1,000 overlapping sessions in a single 24-hour day
        val dayStart = Instant.parse("2026-09-15T00:00:00Z")
        val dayEnd = Instant.parse("2026-09-16T00:00:00Z")
        val random = Random(123)
        val activityIds = listOf("Act_Work", "Act_Study", "Act_Workout", "Act_Reading", "Act_Coding")

        val daySessions = ArrayList<ActivitySessionEntity>(count)
        for (i in 0 until count) {
            val offsetMinutes = random.nextLong(0, 1400)
            val durationMinutes = random.nextLong(10, 120)
            val startTime = dayStart.plus(offsetMinutes, ChronoUnit.MINUTES)
            val endTime = startTime.plus(durationMinutes, ChronoUnit.MINUTES)

            daySessions.add(
                ActivitySessionEntity(
                    id = "heavy_session_$i",
                    activityId = activityIds[random.nextInt(activityIds.size)],
                    startedAt = startTime,
                    endedAt = endTime,
                    createdAt = startTime,
                    updatedAt = startTime
                )
            )
        }

        val currentTime = dayEnd.plus(1, ChronoUnit.HOURS)

        val durationMillis = measureTimeMillis {
            val dailyAnalytics = AnalyticsEngine.calculateDailyAnalytics(
                sessions = daySessions,
                currentTime = currentTime,
                boundaryStart = dayStart,
                boundaryEnd = dayEnd
            )
            assertTrue(dailyAnalytics.timelineSegments.isNotEmpty())
            assertTrue(dailyAnalytics.trackedDuration.inWholeMilliseconds > 0)
        }

        println("=== Daily Analytics Heavy Benchmark ===")
        println("Processed $count concurrent daily sessions in: ${durationMillis}ms")

        // Single day with 1,000 overlapping sessions should finish in under 500ms
        assertTrue(
            "Expected daily analytics for 1,000 sessions to complete within 500ms, took ${durationMillis}ms",
            durationMillis < 500
        )
    }
}
