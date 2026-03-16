package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic wrapper for TRMNL API responses.
 *
 * The TRMNL API consistently wraps responses in a `data` field. This generic class provides
 * a single source of truth for this pattern, reducing boilerplate across API models.
 *
 * ## Usage Examples
 *
 * ### Simple Type Alias for List Responses
 * ```kotlin
 * typealias DevicesResponse = ApiResponse<List<Device>>
 * ```
 *
 * ### Simple Type Alias for Object Responses
 * ```kotlin
 * typealias DeviceResponse = ApiResponse<Device>
 * ```
 *
 * ### Complex Responses with Additional Fields
 * For responses that include pagination or other metadata alongside the `data` field,
 * create a dedicated class (e.g., RecipesResponse for pagination data).
 *
 * @param T The type of data being wrapped
 * @property data The response payload (can be a single object, list, or any serializable type)
 *
 * <pre>
 * // API Response JSON:
 * {
 *   "data": {
 *     "id": 123,
 *     "name": "My Device"
 *   }
 * }
 *
 * // Serializes to/from:
 * ApiResponse(data = Device(id = 123, name = "My Device"))
 * </pre>
 */
@Serializable
data class ApiResponse<T>(
    @SerialName("data")
    val data: T,
)
