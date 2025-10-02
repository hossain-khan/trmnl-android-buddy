package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard error response from the TRMNL API.
 *
 * Returned when an API request fails (e.g., 401 Unauthorized, 404 Not Found).
 *
 * Example response:
 * ```json
 * {
 *   "error": "Unauthorized - Invalid API key"
 * }
 * ```
 *
 * @property error Human-readable error message describing what went wrong
 */
@Serializable
data class ApiError(
    @SerialName("error")
    val error: String
)
