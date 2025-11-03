package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API response wrapper for device models list.
 *
 * Wraps the list of device models in a consistent API response format.
 *
 * @property data List of device models
 */
@Serializable
data class DeviceModelsResponse(
    @SerialName("data")
    val data: List<DeviceModel>,
)
