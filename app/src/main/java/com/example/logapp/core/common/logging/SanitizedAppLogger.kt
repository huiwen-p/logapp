package com.example.logapp.core.common.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SanitizedAppLogger @Inject constructor() : CrashReporter {

    private val sensitiveKeyPatterns = setOf(
        "note", "notetext", "email", "password", "token", "auth", "secret"
    )

    private val emailRegex = Regex("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+")

    fun sanitize(message: String): String {
        return emailRegex.replace(message, "[REDACTED_EMAIL]")
    }

    fun sanitizeAttributes(attributes: Map<String, String>): Map<String, String> {
        return attributes.mapValues { (key, value) ->
            val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
            if (sensitiveKeyPatterns.any { normalizedKey.contains(it) }) {
                "[REDACTED]"
            } else {
                sanitize(value)
            }
        }
    }

    override fun recordException(throwable: Throwable, attributes: Map<String, String>) {
        val sanitizedAttrs = sanitizeAttributes(attributes)
        val sanitizedMessage = sanitize(throwable.message ?: throwable.javaClass.simpleName)
        println("[SanitizedAppLogger] Exception: $sanitizedMessage, Attributes: $sanitizedAttrs")
    }

    override fun logBreadcrumb(message: String) {
        val sanitized = sanitize(message)
        println("[SanitizedAppLogger] Breadcrumb: $sanitized")
    }

    override fun setCustomKey(key: String, value: String) {
        val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
        val sanitizedValue = if (sensitiveKeyPatterns.any { normalizedKey.contains(it) }) {
            "[REDACTED]"
        } else {
            sanitize(value)
        }
        println("[SanitizedAppLogger] CustomKey: $key = $sanitizedValue")
    }
}
