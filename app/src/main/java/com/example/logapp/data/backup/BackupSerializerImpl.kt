package com.example.logapp.data.backup

import com.example.logapp.domain.backup.BackupSerializer
import com.example.logapp.domain.model.backup.BackupSchemaV1
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class BackupSerializerImpl @Inject constructor() : BackupSerializer {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun exportToJson(data: BackupSchemaV1): String {
        return json.encodeToString(data)
    }

    override fun importFromJson(jsonString: String): BackupSchemaV1 {
        return json.decodeFromString(jsonString)
    }
}
