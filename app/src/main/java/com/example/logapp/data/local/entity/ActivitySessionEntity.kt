package com.example.logapp.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Immutable
@Entity(
    tableName = "activity_sessions",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["endedAt"]),
        Index(value = ["activityId"]),
        Index(value = ["deletedAt"]),
        Index(value = ["startedAt", "endedAt"])
    ]
)
data class ActivitySessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val activityId: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val version: Int = 1
)
