package com.example.logapp.data.remote.model

data class SessionDocument(
    val id: String = "",
    val activityId: String = "",
    val startedAt: Long = 0,
    val endedAt: Long? = null,
    val note: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
    val version: Int = 1
)
