package com.example.logapp.core.common.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizedAppLoggerTest {

    private val logger = SanitizedAppLogger()

    @Test
    fun testSanitize_masksEmailAddresses() {
        val input = "Failed to sync user test.user@gmail.com with server"
        val output = logger.sanitize(input)
        assertEquals("Failed to sync user [REDACTED_EMAIL] with server", output)
    }

    @Test
    fun testSanitizeAttributes_redactsSensitiveKeys() {
        val attrs = mapOf(
            "screen" to "TimelineScreen",
            "note" to "My secret journal entry",
            "note_text" to "Confidential message",
            "user_email" to "john.doe@domain.com",
            "session_id" to "123-abc"
        )

        val sanitized = logger.sanitizeAttributes(attrs)

        assertEquals("TimelineScreen", sanitized["screen"])
        assertEquals("[REDACTED]", sanitized["note"])
        assertEquals("[REDACTED]", sanitized["note_text"])
        assertEquals("[REDACTED]", sanitized["user_email"])
        assertEquals("123-abc", sanitized["session_id"])
    }

    @Test
    fun testRecordException_doesNotThrow() {
        val exception = IllegalStateException("Error for user alice@work.org")
        logger.recordException(exception, mapOf("note" to "Private notes", "action" to "SaveSession"))
        logger.logBreadcrumb("Navigating from Home to user@example.com settings")
        logger.setCustomKey("note", "sensitive text")
        logger.setCustomKey("category", "Work")
    }
}
