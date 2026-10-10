package com.example.logapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logapp.data.local.TimeLensDatabase
import com.example.logapp.data.local.entity.ActivitySessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ActivitySessionDaoTest {
    private lateinit var db: TimeLensDatabase
    private lateinit var dao: com.example.logapp.data.local.dao.SessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, TimeLensDatabase::class.java
        ).build()
        dao = db.sessionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadSession() = runBlocking {
        val session = ActivitySessionEntity(
            id = "test-session-1",
            activityId = "activity-1",
            startedAt = Instant.now(),
            endedAt = null,
            note = "Test Note",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 1
        )
        
        dao.insert(session)
        val sessions = dao.getAllSessions().first()
        
        assertEquals(1, sessions.size)
        assertEquals("test-session-1", sessions[0].id)
        assertEquals("Test Note", sessions[0].note)
    }

    @Test
    fun softDeleteSession_isExcludedFromQueries() = runBlocking {
        val session1 = ActivitySessionEntity(
            id = "s1", activityId = "a1", startedAt = Instant.now(),
            createdAt = Instant.now(), updatedAt = Instant.now(), version = 1
        )
        val session2 = ActivitySessionEntity(
            id = "s2", activityId = "a1", startedAt = Instant.now(),
            deletedAt = Instant.now(), // Soft deleted
            createdAt = Instant.now(), updatedAt = Instant.now(), version = 1
        )
        
        dao.insert(session1)
        dao.insert(session2)
        
        val sessions = dao.getAllSessions().first()
        
        assertEquals(1, sessions.size)
        assertEquals("s1", sessions[0].id)
    }
}
