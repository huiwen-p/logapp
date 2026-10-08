package com.example.logapp.domain.engine

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object TimeEngine {
    /**
     * Calculates the start of a "work day" based on a boundary hour.
     * E.g., if boundaryHour = 4, the day starts at 04:00 AM.
     * An instant at 02:00 AM on Sep 30 would belong to the work day starting at 04:00 AM on Sep 29.
     */
    fun getStartOfDay(instant: Instant, boundaryHour: Int = 0, zoneId: ZoneId = ZoneId.systemDefault()): Instant {
        val zdt = instant.atZone(zoneId)
        val adjustedZdt = if (zdt.hour < boundaryHour) {
            zdt.minusDays(1)
        } else {
            zdt
        }
        return adjustedZdt.withHour(boundaryHour).withMinute(0).withSecond(0).withNano(0).toInstant()
    }

    /**
     * Calculates the end of the work day (the exact boundary hour of the next day).
     */
    fun getEndOfDay(instant: Instant, boundaryHour: Int = 0, zoneId: ZoneId = ZoneId.systemDefault()): Instant {
        val startOfDay = getStartOfDay(instant, boundaryHour, zoneId).atZone(zoneId)
        return startOfDay.plusDays(1).toInstant()
    }
}
