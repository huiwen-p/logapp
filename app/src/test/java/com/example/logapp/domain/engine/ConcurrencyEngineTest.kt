package com.example.logapp.domain.engine

import com.example.logapp.domain.model.TimelineSegment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class ConcurrencyEngineTest {

    @Test
    fun testConcurrencyDistribution() {
        val t0 = Instant.parse("2026-09-30T08:00:00Z")
        val t1 = Instant.parse("2026-09-30T08:30:00Z") // 30m
        val t2 = Instant.parse("2026-09-30T08:45:00Z") // 15m
        val t3 = Instant.parse("2026-09-30T09:00:00Z") // 15m
        val t4 = Instant.parse("2026-09-30T09:30:00Z") // 30m
        val t5 = Instant.parse("2026-09-30T10:00:00Z") // 30m
        
        val segments = listOf(
            TimelineSegment(t0, t1, setOf("A"), setOf("Act1")), // 30m, 1 activity
            TimelineSegment(t1, t2, setOf("A", "B"), setOf("Act1", "Act2")), // 15m, 2 activities
            TimelineSegment(t2, t3, setOf("A", "B", "C"), setOf("Act1", "Act2", "Act3")), // 15m, 3 activities
            TimelineSegment(t3, t4, setOf("A", "C"), setOf("Act1", "Act3")), // 30m, 2 activities
            TimelineSegment(t4, t5, setOf("A"), setOf("Act1")) // 30m, 1 activity
        )
        
        val distribution = ConcurrencyEngine.calculateConcurrencyDistribution(segments)
        
        // Level 1: 30m + 30m = 60m
        assertEquals((60 * 60 * 1000).toLong().milliseconds, distribution[1])
        // Level 2: 15m + 30m = 45m
        assertEquals((45 * 60 * 1000).toLong().milliseconds, distribution[2])
        // Level 3: 15m = 15m
        assertEquals((15 * 60 * 1000).toLong().milliseconds, distribution[3])
        // Level 4: 0
        assertEquals(null, distribution[4])
    }
    
    @Test
    fun testConcurrencyDistribution_fourOrMore() {
        val t0 = Instant.parse("2026-09-30T08:00:00Z")
        val t1 = Instant.parse("2026-09-30T08:30:00Z") // 30m
        
        val segments = listOf(
            TimelineSegment(t0, t1, setOf("A", "B", "C", "D", "E"), setOf("Act1", "Act2", "Act3", "Act4", "Act5"))
        )
        
        val distribution = ConcurrencyEngine.calculateConcurrencyDistribution(segments)
        
        assertEquals((30 * 60 * 1000).toLong().milliseconds, distribution[4])
    }

    @Test
    fun testUniqueClockDuration() {
        val t0 = Instant.parse("2026-09-30T08:00:00Z")
        val t1 = Instant.parse("2026-09-30T08:30:00Z") // 30m
        val t2 = Instant.parse("2026-09-30T08:45:00Z") // 15m
        
        val segments = listOf(
            TimelineSegment(t0, t1, setOf("A"), setOf("Act1")),
            TimelineSegment(t1, t2, setOf("A", "B"), setOf("Act1", "Act2"))
        )
        
        val unique = ConcurrencyEngine.calculateUniqueClockDuration(segments)
        assertEquals((45 * 60 * 1000).toLong().milliseconds, unique)
    }
}
