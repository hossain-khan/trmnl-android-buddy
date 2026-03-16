package ink.trmnl.android.buddy.api.util

import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError

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
 *     401 -> "Unauthorized. Please check your access credentials."
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
     * For [ApiResult.Failure.ApiFailure], the human-readable [ApiError.error] field is used
     * when available, avoiding raw `toString()` output in the UI.
     *
     * @param failure The API failure to map
     * @return A user-friendly error message suitable for display in the UI
     */
    fun mapToUserMessage(failure: ApiResult.Failure<*>): String =
        when (failure) {
            is ApiResult.Failure.HttpFailure -> mapHttpError(failure.code)
            is ApiResult.Failure.NetworkFailure -> "Network error. Please check your connection."
            is ApiResult.Failure.ApiFailure<*> ->
                when (val error = failure.error) {
                    is ApiError -> error.error
                    null -> "An error occurred."
                    else -> error.toString()
                }
            is ApiResult.Failure.UnknownFailure -> "Unexpected error. Please try again."
        }

    /**
     * Maps HTTP error codes to user-friendly messages.
     *
     * The 401 message is intentionally auth-type neutral so it applies correctly
     * regardless of whether the caller uses an account API token or a device access key.
     *
     * @param code The HTTP status code
     * @return A user-friendly message for the given HTTP status code
     */
    fun mapHttpError(code: Int): String =
        when (code) {
            401 -> "Unauthorized. Please check your access credentials."
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
