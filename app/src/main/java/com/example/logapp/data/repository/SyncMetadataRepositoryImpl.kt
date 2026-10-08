package com.example.logapp.data.repository

import com.example.logapp.data.local.dao.SyncMetadataDao
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.domain.repository.SyncMetadataRepository

class SyncMetadataRepositoryImpl(
    private val dao: SyncMetadataDao
) : SyncMetadataRepository {
    override suspend fun insertMetadata(metadata: SyncMetadataEntity) = dao.insert(metadata)
    override suspend fun updateMetadata(metadata: SyncMetadataEntity) = dao.update(metadata)
    override suspend fun getPendingSyncs(): List<SyncMetadataEntity> = dao.getPendingSyncs()
}
