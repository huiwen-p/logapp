package com.example.logapp.domain.backup

import com.example.logapp.domain.model.backup.BackupActivity
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.model.backup.BackupSession
import org.junit.Test
import java.time.Instant

class ImportValidationEngineTest {

    private val engine = ImportValidationEngine()

    @Test
    fun testValidSchema() {
        val now = Instant.now()
        val schema = BackupSchemaV1(
            version = 1, exportTime = now,
            activities = listOf(
                BackupActivity("A1", "Act", sortOrder = 0, isArchived = false, createdAt = now, updatedAt = now, version = 1)
            ),
            sessions = listOf(
                BackupSession("S1", "A1", startedAt = now, endedAt = now.plusSeconds(3600), createdAt = now, updatedAt = now, version = 1)
            )
        )
        
        // Should not throw
        engine.validate(schema)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidVersion() {
        val schema = BackupSchemaV1(version = 2, exportTime = Instant.now(), activities = emptyList(), sessions = emptyList())
        engine.validate(schema)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMissingActivityReference() {
        val now = Instant.now()
        val schema = BackupSchemaV1(
            version = 1, exportTime = now,
            activities = emptyList(), // S1 points to A1 which doesn't exist
            sessions = listOf(
                BackupSession("S1", "A1", startedAt = now, createdAt = now, updatedAt = now, version = 1)
            )
        )
        engine.validate(schema)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidTimestamps() {
        val now = Instant.now()
        val schema = BackupSchemaV1(
            version = 1, exportTime = now,
            activities = listOf(
                BackupActivity("A1", "Act", sortOrder = 0, isArchived = false, createdAt = now, updatedAt = now, version = 1)
            ),
            sessions = listOf(
                // startedAt > endedAt
                BackupSession("S1", "A1", startedAt = now.plusSeconds(3600), endedAt = now, createdAt = now, updatedAt = now, version = 1)
            )
        )
        engine.validate(schema)
    }
}
