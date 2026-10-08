package com.example.logapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "activity_sessions")
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
