package com.example.logapp.domain.model.backup

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class BackupSchemaV1(
    val version: Int = 1,
    @Serializable(with = InstantSerializer::class) val exportTime: Instant,
    val activities: List<BackupActivity>,
    val sessions: List<BackupSession>
)

@Serializable
data class BackupActivity(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: Long? = null,
    val categoryId: String? = null,
    val sortOrder: Int,
    val isArchived: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
    val version: Int
)

@Serializable
data class BackupSession(
    val id: String,
    val activityId: String,
    @Serializable(with = InstantSerializer::class) val startedAt: Instant,
    @Serializable(with = InstantSerializer::class) val endedAt: Instant? = null,
    val note: String? = null,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
    @Serializable(with = InstantSerializer::class) val deletedAt: Instant? = null,
    val version: Int
)
