package com.example.logapp.core.common.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun testAppResult_successFlow() {
        val result = AppResult.Success("test_data")
        assertTrue(result.isSuccess)
        assertEquals("test_data", result.getOrNull())
        assertEquals("test_data", result.getOrDefault("fallback"))
        assertNull(result.errorOrNull())

        val mapped = result.map { it.length }
        assertTrue(mapped is AppResult.Success)
        assertEquals(9, (mapped as AppResult.Success).data)
    }

    @Test
    fun testAppResult_failureFlow() {
        val error = AppError.ValidationError("Field required", "name")
        val result: AppResult<String> = AppResult.Failure(error)

        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals("fallback", result.getOrDefault("fallback"))
        assertEquals(error, result.errorOrNull())

        val mapped = result.map { it.length }
        assertTrue(mapped is AppResult.Failure)
    }

    @Test
    fun testAppResult_runCatching() {
        val successResult = AppResult.runCatching {
            "hello"
        }
        assertTrue(successResult is AppResult.Success)
        assertEquals("hello", successResult.getOrNull())

        val failureResult = AppResult.runCatching {
            throw IllegalArgumentException("Invalid input argument")
        }
        assertTrue(failureResult is AppResult.Failure)
        val err = (failureResult as AppResult.Failure).error
        assertTrue(err is AppError.ValidationError)
    }
}
