package com.example.logapp.feature.timeline

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.feature.timeline.engine.TimelineLayoutEngine
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TimelineLayoutEngineTest {

    private fun createSession(
        id: String,
        startStr: String,
        endStr: String?
    ): ActivitySessionEntity {
        return ActivitySessionEntity(
            id = id,
            activityId = "act1",
            startedAt = Instant.parse(startStr),
            endedAt = endStr?.let { Instant.parse(it) },
            createdAt = Instant.parse(startStr),
            updatedAt = Instant.parse(startStr),
            version = 1
        )
    }

    @Test
    fun `isolated sessions take full width and are not overlapping`() {
        val s1 = createSession("s1", "2026-10-11T08:00:00Z", "2026-10-11T09:00:00Z")
        val s2 = createSession("s2", "2026-10-11T10:00:00Z", "2026-10-11T11:00:00Z")

        val layout = TimelineLayoutEngine.calculateLayout(listOf(s1, s2))

        val p1 = layout["s1"]!!
        val p2 = layout["s2"]!!

        assertEquals(0, p1.columnIndex)
        assertEquals(1, p1.totalColumns)
        assertFalse(p1.isOverlapping)

        assertEquals(0, p2.columnIndex)
        assertEquals(1, p2.totalColumns)
        assertFalse(p2.isOverlapping)
    }

    @Test
    fun `back to back touching sessions do not overlap and each take full width`() {
        val s1 = createSession("s1", "2026-10-11T08:00:00Z", "2026-10-11T09:00:00Z")
        val s2 = createSession("s2", "2026-10-11T09:00:00Z", "2026-10-11T10:00:00Z")

        val layout = TimelineLayoutEngine.calculateLayout(listOf(s1, s2))

        val p1 = layout["s1"]!!
        val p2 = layout["s2"]!!

        assertEquals(1, p1.totalColumns)
        assertFalse(p1.isOverlapping)

        assertEquals(1, p2.totalColumns)
        assertFalse(p2.isOverlapping)
    }

    @Test
    fun `overlapping sessions form a cluster with allocated columns`() {
        val s1 = createSession("s1", "2026-10-11T08:00:00Z", "2026-10-11T09:30:00Z")
        val s2 = createSession("s2", "2026-10-11T08:30:00Z", "2026-10-11T10:00:00Z")

        val layout = TimelineLayoutEngine.calculateLayout(listOf(s1, s2))

        val p1 = layout["s1"]!!
        val p2 = layout["s2"]!!

        assertEquals(2, p1.totalColumns)
        assertEquals(2, p2.totalColumns)
        assertEquals(0, p1.columnIndex)
        assertEquals(1, p2.columnIndex)
        assertTrue(p1.isOverlapping)
        assertTrue(p2.isOverlapping)
    }

    @Test
    fun `mixed cluster layout preserves full width for non-overlapping sessions`() {
        // Morning: single isolated session
        val sMorning = createSession("morning", "2026-10-11T07:00:00Z", "2026-10-11T08:00:00Z")
        // Afternoon: 3 overlapping sessions
        val sA1 = createSession("a1", "2026-10-11T13:00:00Z", "2026-10-11T14:30:00Z")
        val sA2 = createSession("a2", "2026-10-11T13:30:00Z", "2026-10-11T15:00:00Z")
        val sA3 = createSession("a3", "2026-10-11T14:00:00Z", "2026-10-11T15:30:00Z")
        // Evening: single isolated session
        val sEvening = createSession("evening", "2026-10-11T19:00:00Z", "2026-10-11T20:00:00Z")

        val layout = TimelineLayoutEngine.calculateLayout(listOf(sMorning, sA1, sA2, sA3, sEvening))

        // Morning and Evening must take 100% width (totalColumns = 1)
        val pMorning = layout["morning"]!!
        assertEquals(1, pMorning.totalColumns)
        assertFalse(pMorning.isOverlapping)

        val pEvening = layout["evening"]!!
        assertEquals(1, pEvening.totalColumns)
        assertFalse(pEvening.isOverlapping)

        // Afternoon sessions share 3 columns
        val pA1 = layout["a1"]!!
        val pA2 = layout["a2"]!!
        val pA3 = layout["a3"]!!

        assertEquals(3, pA1.totalColumns)
        assertEquals(3, pA2.totalColumns)
        assertEquals(3, pA3.totalColumns)

        assertTrue(pA1.isOverlapping)
        assertTrue(pA2.isOverlapping)
        assertTrue(pA3.isOverlapping)
    }

    @Test
    fun `empty session list produces empty map`() {
        val layout = TimelineLayoutEngine.calculateLayout(emptyList())
        assertTrue(layout.isEmpty())
    }
}
