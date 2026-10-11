package com.example.logapp.core.common.result

/**
 * Domain-level error hierarchy representing failure types across application layers.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class ValidationError(
        override val message: String,
        val field: String? = null
    ) : AppError(message)

    data class NotFoundError(
        val entityName: String,
        val identifier: String
    ) : AppError("$entityName with identifier '$identifier' not found.")

    data class ConflictError(
        override val message: String
    ) : AppError(message)

    data class HealthCheckError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    companion object {
        fun fromThrowable(throwable: Throwable): AppError {
            val className = throwable::class.qualifiedName ?: ""
            return when {
                className.contains("SQLException") || className.contains("SQLite") -> {
                    DatabaseError(
                        message = throwable.localizedMessage ?: "Database operation failed",
                        cause = throwable
                    )
                }
                throwable is IllegalArgumentException || throwable is IllegalStateException -> {
                    ValidationError(
                        message = throwable.localizedMessage ?: "Validation failed"
                    )
                }
                else -> {
                    UnknownError(
                        message = throwable.localizedMessage ?: "Unknown error",
                        cause = throwable
                    )
                }
            }
        }
    }
}
