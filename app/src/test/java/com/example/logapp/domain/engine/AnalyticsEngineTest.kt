package com.example.logapp.domain.engine

import com.example.logapp.data.local.entity.ActivitySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class AnalyticsEngineTest {

    private fun createSession(id: String, activityId: String, start: String, end: String): ActivitySessionEntity {
        return ActivitySessionEntity(
            id = id,
            activityId = activityId,
            startedAt = Instant.parse(start),
            endedAt = Instant.parse(end),
            createdAt = Instant.parse(start),
            updatedAt = Instant.parse(start)
        )
    }

    @Test
    fun testOverlap_SRS76_Case1_NoOverlap() {
        val currentTime = Instant.parse("2026-09-30T12:00:00Z")
        // A 08:00–10:00, B 11:00–12:00
        val sessionA = createSession("A", "Act1", "2026-09-30T08:00:00Z", "2026-09-30T10:00:00Z")
        val sessionB = createSession("B", "Act2", "2026-09-30T11:00:00Z", "2026-09-30T12:00:00Z")

        val analytics = AnalyticsEngine.calculateDailyAnalytics(listOf(sessionA, sessionB), currentTime)

        // Overlap = 0
        assertEquals(0L.milliseconds, analytics.overlapDuration)
        assertEquals((3 * 60 * 60 * 1000L).milliseconds, analytics.trackedDuration)
        assertEquals((3 * 60 * 60 * 1000L).milliseconds, analytics.uniqueClockDuration)
    }

    @Test
    fun testOverlap_SRS76_Case2_OneHourOverlap() {
        val currentTime = Instant.parse("2026-09-30T12:00:00Z")
        // A 08:00–10:00, B 09:00–11:00
        val sessionA = createSession("A", "Act1", "2026-09-30T08:00:00Z", "2026-09-30T10:00:00Z")
        val sessionB = createSession("B", "Act2", "2026-09-30T09:00:00Z", "2026-09-30T11:00:00Z")

        val analytics = AnalyticsEngine.calculateDailyAnalytics(listOf(sessionA, sessionB), currentTime)

        // Overlap = 1h
        assertEquals((60 * 60 * 1000L).milliseconds, analytics.overlapDuration)
        // Tracked = 2h + 2h = 4h
        assertEquals((4 * 60 * 60 * 1000L).milliseconds, analytics.trackedDuration)
        // Unique = 3h (08:00 to 11:00)
        assertEquals((3 * 60 * 60 * 1000L).milliseconds, analytics.uniqueClockDuration)
    }

    @Test
    fun testOverlap_SRS76_Case3_ThirtyMinOverlap() {
        val currentTime = Instant.parse("2026-09-30T12:00:00Z")
        // A 08:00–10:00, B 08:30–09:00
        val sessionA = createSession("A", "Act1", "2026-09-30T08:00:00Z", "2026-09-30T10:00:00Z")
        val sessionB = createSession("B", "Act2", "2026-09-30T08:30:00Z", "2026-09-30T09:00:00Z")

        val analytics = AnalyticsEngine.calculateDailyAnalytics(listOf(sessionA, sessionB), currentTime)

        // Overlap = 30m
        assertEquals((30 * 60 * 1000L).milliseconds, analytics.overlapDuration)
        // Tracked = 2h + 30m = 2.5h
        assertEquals((150 * 60 * 1000L).milliseconds, analytics.trackedDuration)
        // Unique = 2h (08:00 to 10:00)
        assertEquals((120 * 60 * 1000L).milliseconds, analytics.uniqueClockDuration)
    }

    @Test
    fun testOverlap_SRS76_Case4_ConcurrencySegments() {
        val currentTime = Instant.parse("2026-09-30T12:00:00Z")
        // A 08:00–10:00
        // B 08:30–09:00
        // C 08:45–09:30
        val sessionA = createSession("A", "Act1", "2026-09-30T08:00:00Z", "2026-09-30T10:00:00Z")
        val sessionB = createSession("B", "Act2", "2026-09-30T08:30:00Z", "2026-09-30T09:00:00Z")
        val sessionC = createSession("C", "Act3", "2026-09-30T08:45:00Z", "2026-09-30T09:30:00Z")

        val analytics = AnalyticsEngine.calculateDailyAnalytics(listOf(sessionA, sessionB, sessionC), currentTime)
        
        // Let's verify segments
        // 08:00 - 08:30: A (30m, level 1)
        // 08:30 - 08:45: A, B (15m, level 2)
        // 08:45 - 09:00: A, B, C (15m, level 3)
        // 09:00 - 09:30: A, C (30m, level 2)
        // 09:30 - 10:00: A (30m, level 1)
        
        assertEquals((60 * 60 * 1000L).milliseconds, analytics.concurrencyDistribution[1]) // 30 + 30 = 60m
        assertEquals((45 * 60 * 1000L).milliseconds, analytics.concurrencyDistribution[2]) // 15 + 30 = 45m
        assertEquals((15 * 60 * 1000L).milliseconds, analytics.concurrencyDistribution[3]) // 15m
    }
}
