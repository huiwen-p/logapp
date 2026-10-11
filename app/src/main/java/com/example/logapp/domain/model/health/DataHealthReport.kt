package com.example.logapp.domain.model.health

import java.time.Instant

data class DataHealthReport(
    val totalActivities: Int,
    val totalSessions: Int,
    val orphanSessionsCount: Int,
    val invalidTimestampCount: Int,
    val checkedAt: Instant = Instant.now()
) {
    val isHealthy: Boolean
        get() = orphanSessionsCount == 0 && invalidTimestampCount == 0

    val hasIssues: Boolean
        get() = !isHealthy
}
