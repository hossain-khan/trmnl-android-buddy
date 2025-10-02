package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response wrapper for the `/me` API endpoint.
 *
 * The TRMNL API wraps single object responses in a `data` field for consistency.
 *
 * @property data The user data
 */
@Serializable
data class UserResponse(
    @SerialName("data")
    val data: User,
)
