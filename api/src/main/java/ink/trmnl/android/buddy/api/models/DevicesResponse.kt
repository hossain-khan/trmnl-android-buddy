package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response wrapper for the `/devices` API endpoint.
 *
 * The TRMNL API wraps list responses in a `data` field for consistency.
 *
 * Example response:
 * ```json
 * {
 *   "data": [
 *     {
 *       "id": 12345,
 *       "name": "Kitchen Display",
 *       "friendly_id": "ABC123",
 *       "mac_address": "00:11:22:33:44:55",
 *       "battery_voltage": 3.8,
 *       "rssi": -27,
 *       "percent_charged": 66.67,
 *       "wifi_strength": 100
 *     }
 *   ]
 * }
 * ```
 *
 * @property data List of devices belonging to the authenticated user
 */
@Serializable
data class DevicesResponse(
    @SerialName("data")
    val data: List<Device>
)
