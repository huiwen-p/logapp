package com.example.logapp.domain.backup

import com.example.logapp.domain.model.backup.BackupSchemaV1

interface BackupSerializer {
    fun exportToJson(data: BackupSchemaV1): String
    fun importFromJson(json: String): BackupSchemaV1
}
