package com.example.logapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val entityId: String,
    val entityType: String, // e.g., "ACTIVITY", "SESSION", "DAILY_NOTE"
    val operation: String,  // e.g., "INSERT", "UPDATE", "DELETE"
    val localVersion: Int,
    val syncStatus: SyncStatus,
    val lastAttemptAt: Instant? = null,
    val lastSyncedAt: Instant? = null,
    val error: String? = null
)
