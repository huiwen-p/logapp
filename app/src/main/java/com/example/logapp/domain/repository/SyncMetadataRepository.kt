package com.example.logapp.domain.repository

import com.example.logapp.data.local.entity.SyncMetadataEntity

interface SyncMetadataRepository {
    suspend fun insertMetadata(metadata: SyncMetadataEntity)
    suspend fun updateMetadata(metadata: SyncMetadataEntity)
    suspend fun getPendingSyncs(): List<SyncMetadataEntity>
}
