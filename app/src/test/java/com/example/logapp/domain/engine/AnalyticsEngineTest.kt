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

    @Test
    fun testDayBoundaryTruncation() {
        val currentTime = Instant.parse("2026-09-30T12:00:00Z")
        val boundaryStart = Instant.parse("2026-09-30T04:00:00Z")
        val boundaryEnd = Instant.parse("2026-10-01T04:00:00Z")
        
        // Session starting before boundary and ending inside
        val sessionA = createSession("A", "Act1", "2026-09-30T02:00:00Z", "2026-09-30T05:00:00Z")
        // Session completely outside boundary (before)
        val sessionB = createSession("B", "Act2", "2026-09-30T01:00:00Z", "2026-09-30T03:00:00Z")
        // Session inside boundary
        val sessionC = createSession("C", "Act3", "2026-09-30T05:00:00Z", "2026-09-30T06:00:00Z")

        val analytics = AnalyticsEngine.calculateDailyAnalytics(
            listOf(sessionA, sessionB, sessionC),
            currentTime,
            boundaryStart,
            boundaryEnd
        )

        // Session B should be ignored.
        // Session A should be truncated to 04:00 - 05:00 (1 hour).
        // Session C remains 05:00 - 06:00 (1 hour).
        // Total tracked duration should be 2 hours.
        assertEquals((2 * 60 * 60 * 1000L).milliseconds, analytics.trackedDuration)
    }

    @Test
    fun testActivityRelationships() {
        val currentTime = Instant.parse("2026-09-30T12:00:00Z")
        // A 08:00–10:00 (Act1)
        // B 08:30–09:00 (Act2)
        // C 08:45–09:30 (Act3)
        // Overlaps: 
        // Act1 & Act2: 08:30-09:00 (30m)
        // Act1 & Act3: 08:45-09:30 (45m)
        // Act2 & Act3: 08:45-09:00 (15m)
        
        val sessionA = createSession("A", "Act1", "2026-09-30T08:00:00Z", "2026-09-30T10:00:00Z")
        val sessionB = createSession("B", "Act2", "2026-09-30T08:30:00Z", "2026-09-30T09:00:00Z")
        val sessionC = createSession("C", "Act3", "2026-09-30T08:45:00Z", "2026-09-30T09:30:00Z")

        val analytics = AnalyticsEngine.calculateDailyAnalytics(listOf(sessionA, sessionB, sessionC), currentTime)
        
        val rels = analytics.activityRelationships
        
        val p12 = if ("Act1" < "Act2") Pair("Act1", "Act2") else Pair("Act2", "Act1")
        val p13 = if ("Act1" < "Act3") Pair("Act1", "Act3") else Pair("Act3", "Act1")
        val p23 = if ("Act2" < "Act3") Pair("Act2", "Act3") else Pair("Act3", "Act2")
        
        assertEquals((30 * 60 * 1000L).milliseconds, rels[p12])
        assertEquals((45 * 60 * 1000L).milliseconds, rels[p13])
        assertEquals((15 * 60 * 1000L).milliseconds, rels[p23])
    }

    @Test
    fun testWeeklyBoundaries() {
        val currentTime = Instant.parse("2026-10-02T12:00:00Z")
        val session1 = createSession("S1", "Act1", "2026-10-01T02:00:00Z", "2026-10-01T05:00:00Z")
        val session2 = createSession("S2", "Act2", "2026-10-02T05:00:00Z", "2026-10-02T06:00:00Z")

        val date1 = java.time.LocalDate.of(2026, 9, 30)
        val date2 = java.time.LocalDate.of(2026, 10, 1)

        // Boundary is 04:00 AM UTC
        val bounds = mapOf(
            date1 to Pair(Instant.parse("2026-09-30T04:00:00Z"), Instant.parse("2026-10-01T04:00:00Z")),
            date2 to Pair(Instant.parse("2026-10-01T04:00:00Z"), Instant.parse("2026-10-02T04:00:00Z"))
        )

        val analytics = AnalyticsEngine.calculateWeeklyAnalytics(listOf(session1, session2), currentTime, bounds)
        
        // session1 (02:00 to 05:00 on 10-01). 
        // For date1 (ends at 04:00): segment is 02:00 to 04:00 (2h)
        // For date2 (starts at 04:00): segment is 04:00 to 05:00 (1h)
        val day1Analytics = analytics.dailyBreakdown[date1]!!
        val day2Analytics = analytics.dailyBreakdown[date2]!!

        assertEquals((2 * 60 * 60 * 1000L).milliseconds, day1Analytics.trackedDuration)
        assertEquals((1 * 60 * 60 * 1000L).milliseconds, day2Analytics.trackedDuration)
        
        // Session 2 is completely out of the provided bounds, so it's ignored
        assertEquals((3 * 60 * 60 * 1000L).milliseconds, analytics.totalTrackedDuration)
    }
}
