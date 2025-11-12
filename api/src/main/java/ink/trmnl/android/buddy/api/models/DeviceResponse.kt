package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response wrapper for the `/devices/{id}` API endpoint.
 *
 * The TRMNL API wraps single object responses in a `data` field for consistency.
 *
 * @property data The device data
 */
@Serializable
data class DeviceResponse(
    @SerialName("data")
    val data: Device,
)
