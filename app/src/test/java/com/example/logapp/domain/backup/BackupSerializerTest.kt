package com.example.logapp.domain.backup

import com.example.logapp.data.backup.BackupSerializerImpl
import com.example.logapp.domain.model.backup.BackupActivity
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.model.backup.BackupSession
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class BackupSerializerTest {

    @Test
    fun testSerializeAndDeserialize() {
        val serializer = BackupSerializerImpl()

        val now = Instant.parse("2026-09-30T10:00:00Z")
        val activity = BackupActivity(
            id = "A1", name = "Test Activity", sortOrder = 0, isArchived = false,
            createdAt = now, updatedAt = now, version = 1
        )
        val session = BackupSession(
            id = "S1", activityId = "A1", startedAt = now, endedAt = null,
            createdAt = now, updatedAt = now, version = 1
        )
        val schema = BackupSchemaV1(
            version = 1, exportTime = now, activities = listOf(activity), sessions = listOf(session)
        )

        val json = serializer.exportToJson(schema)
        assertTrue(json.contains("\"Test Activity\""))
        assertTrue(json.contains("\"2026-09-30T10:00:00Z\""))

        val deserialized = serializer.importFromJson(json)
        assertEquals(schema.version, deserialized.version)
        assertEquals(1, deserialized.activities.size)
        assertEquals("A1", deserialized.activities[0].id)
        assertEquals(now, deserialized.sessions[0].startedAt)
    }
}
