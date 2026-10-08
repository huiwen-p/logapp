package com.example.logapp.domain.engine

import com.example.logapp.data.local.entity.ActivitySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class OverlapEngineTest {

    @Test
    fun testOverlapCalculation_noOverlap() {
        val currentTime = Instant.parse("2026-09-30T10:00:00Z")
        
        // Session A: 08:00 - 08:40
        val sessionA = ActivitySessionEntity(
            id = "A", activityId = "Act1",
            startedAt = Instant.parse("2026-09-30T08:00:00Z"),
            endedAt = Instant.parse("2026-09-30T08:40:00Z"),
            createdAt = currentTime, updatedAt = currentTime
        )
        
        // Session B: 09:00 - 10:00
        val sessionB = ActivitySessionEntity(
            id = "B", activityId = "Act1",
            startedAt = Instant.parse("2026-09-30T09:00:00Z"),
            endedAt = Instant.parse("2026-09-30T10:00:00Z"),
            createdAt = currentTime, updatedAt = currentTime
        )

        val segments = OverlapEngine.calculateSegments(listOf(sessionA, sessionB), currentTime)

        assertEquals(2, segments.size)
        
        assertEquals(Instant.parse("2026-09-30T08:00:00Z"), segments[0].start)
        assertEquals(Instant.parse("2026-09-30T08:40:00Z"), segments[0].end)
        assertEquals(setOf("A"), segments[0].activeSessionIds)

        assertEquals(Instant.parse("2026-09-30T09:00:00Z"), segments[1].start)
        assertEquals(Instant.parse("2026-09-30T10:00:00Z"), segments[1].end)
        assertEquals(setOf("B"), segments[1].activeSessionIds)
    }

    @Test
    fun testOverlapCalculation_withOverlap() {
        val currentTime = Instant.parse("2026-09-30T10:00:00Z")
        
        // A = 08:00 -> 10:00
        // B = 08:40 -> 09:00
        // C = 08:50 -> 09:30
        
        val sessionA = ActivitySessionEntity(
            id = "A", activityId = "Act1",
            startedAt = Instant.parse("2026-09-30T08:00:00Z"),
            endedAt = Instant.parse("2026-09-30T10:00:00Z"),
            createdAt = currentTime, updatedAt = currentTime
        )
        
        val sessionB = ActivitySessionEntity(
            id = "B", activityId = "Act2",
            startedAt = Instant.parse("2026-09-30T08:40:00Z"),
            endedAt = Instant.parse("2026-09-30T09:00:00Z"),
            createdAt = currentTime, updatedAt = currentTime
        )
        
        val sessionC = ActivitySessionEntity(
            id = "C", activityId = "Act3",
            startedAt = Instant.parse("2026-09-30T08:50:00Z"),
            endedAt = Instant.parse("2026-09-30T09:30:00Z"),
            createdAt = currentTime, updatedAt = currentTime
        )

        val segments = OverlapEngine.calculateSegments(listOf(sessionA, sessionB, sessionC), currentTime)

        // Expected segments:
        // 08:00 -> 08:40 (A)
        // 08:40 -> 08:50 (A, B)
        // 08:50 -> 09:00 (A, B, C)
        // 09:00 -> 09:30 (A, C)
        // 09:30 -> 10:00 (A)
        
        assertEquals(5, segments.size)
        
        assertEquals(setOf("A"), segments[0].activeSessionIds)
        assertEquals(setOf("A", "B"), segments[1].activeSessionIds)
        assertEquals(setOf("A", "B", "C"), segments[2].activeSessionIds)
        assertEquals(setOf("A", "C"), segments[3].activeSessionIds)
        assertEquals(setOf("A"), segments[4].activeSessionIds)
    }
}
