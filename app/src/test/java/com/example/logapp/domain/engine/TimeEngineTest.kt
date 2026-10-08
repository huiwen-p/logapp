package com.example.logapp.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TimeEngineTest {
    
    @Test
    fun testGetStartOfDay_noBoundary() {
        val instant = Instant.parse("2026-09-30T10:00:00Z")
        val zoneId = ZoneId.of("UTC")
        val startOfDay = TimeEngine.getStartOfDay(instant, boundaryHour = 0, zoneId = zoneId)
        
        assertEquals(Instant.parse("2026-09-30T00:00:00Z"), startOfDay)
    }

    @Test
    fun testGetStartOfDay_withBoundary() {
        val zoneId = ZoneId.of("UTC")
        
        // At 02:00 AM on the 30th, if boundary = 4 AM, it still belongs to the 29th's work day
        val instant1 = Instant.parse("2026-09-30T02:00:00Z")
        val startOfDay1 = TimeEngine.getStartOfDay(instant1, boundaryHour = 4, zoneId = zoneId)
        assertEquals(Instant.parse("2026-09-29T04:00:00Z"), startOfDay1)
        
        // At 05:00 AM on the 30th, if boundary = 4 AM, it belongs to the 30th's work day
        val instant2 = Instant.parse("2026-09-30T05:00:00Z")
        val startOfDay2 = TimeEngine.getStartOfDay(instant2, boundaryHour = 4, zoneId = zoneId)
        assertEquals(Instant.parse("2026-09-30T04:00:00Z"), startOfDay2)
    }
}
