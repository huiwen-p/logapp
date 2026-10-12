package com.example.logapp.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Immutable
@Entity(
    tableName = "activities",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["sortOrder"])
    ]
)
data class ActivityEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String? = null,
    val color: Long? = null, // ARGB color
    val categoryId: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val version: Int = 1
)
