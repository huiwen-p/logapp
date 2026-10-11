package com.example.logapp.data.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesManagerTest {

    @Test
    fun parseDayBoundary_validFormats() {
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary("00:00"))
        assertEquals(Pair(4, 0), PreferencesManager.parseDayBoundary("04:00"))
        assertEquals(Pair(23, 59), PreferencesManager.parseDayBoundary("23:59"))
        assertEquals(Pair(7, 30), PreferencesManager.parseDayBoundary("07:30"))
    }

    @Test
    fun parseDayBoundary_incompleteOrSingleNumber() {
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary("0"))
        assertEquals(Pair(4, 0), PreferencesManager.parseDayBoundary("4"))
        assertEquals(Pair(12, 0), PreferencesManager.parseDayBoundary("12"))
    }

    @Test
    fun parseDayBoundary_emptyOrNullOrInvalid() {
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary(null))
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary(""))
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary("   "))
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary("invalid"))
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary("foo:bar"))
    }

    @Test
    fun parseDayBoundary_outOfRangeValuesClamped() {
        assertEquals(Pair(23, 59), PreferencesManager.parseDayBoundary("99:99"))
        assertEquals(Pair(0, 0), PreferencesManager.parseDayBoundary("-5:-10"))
    }
}
