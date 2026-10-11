package com.example.logapp.core.common.logging

/**
 * Abstraction for reporting non-fatal errors, breadcrumbs, and exceptions.
 * Allows decoupling error reporting from specific third-party providers (such as Firebase Crashlytics).
 */
interface CrashReporter {
    /**
     * Record a non-fatal exception after sanitizing sensitive personal data.
     */
    fun recordException(throwable: Throwable, attributes: Map<String, String> = emptyMap())

    /**
     * Log an informative breadcrumb message.
     */
    fun logBreadcrumb(message: String)

    /**
     * Sets a non-sensitive custom attribute/tag.
     */
    fun setCustomKey(key: String, value: String)
}
