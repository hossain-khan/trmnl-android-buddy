package ink.trmnl.android.buddy.api.util

import com.slack.eithernet.ApiResult

/**
 * Utility object for mapping API failures to user-friendly error messages.
 *
 * Centralizes error message generation to ensure consistent error messaging
 * across all presenters and UI components. This eliminates duplicated
 * error-to-message mapping logic spread across multiple presenters.
 *
 * **Usage Example:**
 * ```kotlin
 * // Before (duplicated per presenter)
 * is ApiResult.Failure.HttpFailure -> when (result.code) {
 *     401 -> "Unauthorized. Please check your API token."
 *     else -> "HTTP Error ${result.code}"
 * }
 *
 * // After (using ErrorMapper)
 * is ApiResult.Failure -> errorMessage = ErrorMapper.mapToUserMessage(result)
 * ```
 *
 * @see ApiResult EitherNet's sealed result type
 */
object ErrorMapper {
    /**
     * Maps an [ApiResult.Failure] to a user-friendly error message string.
     *
     * @param failure The API failure to map
     * @return A user-friendly error message suitable for display in the UI
     */
    fun mapToUserMessage(failure: ApiResult.Failure<*>): String =
        when (failure) {
            is ApiResult.Failure.HttpFailure -> mapHttpError(failure.code)
            is ApiResult.Failure.NetworkFailure -> "Network error. Please check your connection."
            is ApiResult.Failure.ApiFailure<*> -> failure.error?.toString() ?: "An error occurred."
            is ApiResult.Failure.UnknownFailure -> "Unexpected error. Please try again."
        }

    /**
     * Maps HTTP error codes to user-friendly messages.
     *
     * @param code The HTTP status code
     * @return A user-friendly message for the given HTTP status code
     */
    fun mapHttpError(code: Int): String =
        when (code) {
            401 -> "Unauthorized. Please check your API token."
            403 -> "Forbidden. You don't have access to this resource."
            404 -> "Resource not found."
            429 -> "Too many requests. Please try again later."
            in 500..599 -> "Server error. Please try again later."
            else -> "HTTP Error $code."
        }
}

/**
 * Extension function to convert an [ApiResult.Failure] to a user-friendly error message.
 *
 * Convenience wrapper around [ErrorMapper.mapToUserMessage].
 *
 * **Usage Example:**
 * ```kotlin
 * is ApiResult.Failure -> errorMessage = result.toUserMessage()
 * ```
 *
 * @return A user-friendly error message suitable for display in the UI
 */
fun ApiResult.Failure<*>.toUserMessage(): String = ErrorMapper.mapToUserMessage(this)
