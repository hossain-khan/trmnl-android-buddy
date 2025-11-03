package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.Serializable

/**
 * Response wrapper for the /api/models endpoint.
 * Contains a list of all supported TRMNL device models.
 */
@Serializable
data class DeviceModelsResponse(
    val data: List<DeviceModel>,
)
