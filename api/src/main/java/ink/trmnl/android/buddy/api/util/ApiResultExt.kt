package ink.trmnl.android.buddy.api.util

import com.slack.eithernet.ApiResult

/*
 * Extension functions for converting [ApiResult] to Kotlin [Result] with standardized error handling.
 *
 * These utilities reduce code duplication across repositories and presenters that need to convert
 * EitherNet's type-safe [ApiResult] into Kotlin's standard [Result] type.
 *
 * **Usage Example:**
 * ```kotlin
 * // Before (duplicated error handling)
 * when (val result = apiService.getDevices(token)) {
 *     is ApiResult.Success -> Result.success(result.value.data)
 *     is ApiResult.Failure.HttpFailure -> Result.failure(Exception("HTTP ${result.code}: ..."))
 *     is ApiResult.Failure.NetworkFailure -> Result.failure(Exception("Network error: ..."))
 *     is ApiResult.Failure.ApiFailure -> Result.failure(Exception("API error: ..."))
 *     is ApiResult.Failure.UnknownFailure -> Result.failure(Exception("Unknown error: ..."))
 * }
 *
 * // After (using extension)
 * apiService.getDevices(token).toResult { it.data }
 * ```
 *
 * @see ApiResult EitherNet's sealed result type
 */

/**
 * Convert [ApiResult] to Kotlin [Result] with a custom success transformer.
 *
 * This is the most flexible version that allows transforming the success value before wrapping it.
 *
 * **Error Mapping:**
 * - [ApiResult.Success] → `Result.success(transform(value))`
 * - [ApiResult.Failure.HttpFailure] → `Result.failure(Exception("HTTP {code}: {context}"))`
 * - [ApiResult.Failure.NetworkFailure] → `Result.failure(Exception("Network error: {message}"))`
 * - [ApiResult.Failure.ApiFailure] → `Result.failure(Exception("API error: {error}"))`
 * - [ApiResult.Failure.UnknownFailure] → `Result.failure(Exception("Unknown error: {message}"))`
 *
 * @param T The success type of the ApiResult
 * @param R The transformed success type for the Result
 * @param context Optional context string for HTTP errors (e.g., "Failed to fetch devices")
 * @param transform Function to transform the success value (e.g., extract nested data)
 * @return Kotlin Result with transformed success value or mapped failure
 *
 * @see toResultDirect For cases where no transformation is needed
 */
inline fun <T : Any, R : Any> ApiResult<T, *>.toResult(
    context: String? = null,
    transform: (T) -> R,
): Result<R> =
    when (this) {
        is ApiResult.Success -> Result.success(transform(value))
        is ApiResult.Failure.HttpFailure -> {
            val message = if (context != null) "HTTP $code: $context" else "HTTP $code"
            Result.failure(Exception(message))
        }
        is ApiResult.Failure.NetworkFailure ->
            Result.failure(
                Exception("Network error: ${error.message ?: "Unknown network issue"}"),
            )
        is ApiResult.Failure.ApiFailure ->
            Result.failure(
                Exception("API error: $error"),
            )
        is ApiResult.Failure.UnknownFailure ->
            Result.failure(
                Exception("Unknown error: ${error.message ?: "Unexpected error occurred"}"),
            )
    }

/**
 * Convert [ApiResult] to Kotlin [Result] without transformation.
 *
 * Use this when the ApiResult success value doesn't need to be transformed.
 *
 * **Usage Example:**
 * ```kotlin
 * apiService.getRecipes().toResultDirect("Failed to fetch recipes")
 * ```
 *
 * @param T The success type (used for both ApiResult and Result)
 * @param context Optional context string for HTTP errors
 * @return Kotlin Result with the original success value or mapped failure
 *
 * @see toResult For cases requiring value transformation
 */
inline fun <T : Any> ApiResult<T, *>.toResultDirect(context: String? = null): Result<T> = toResult(context) { it }
