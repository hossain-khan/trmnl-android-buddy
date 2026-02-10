package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response wrapper for the `/playlists/items` API endpoint.
 *
 * The TRMNL API wraps list responses in a `data` field for consistency.
 *
 * Example response:
 * ```json
 * {
 *   "data": [
 *     {
 *       "id": 491784,
 *       "device_id": 41448,
 *       "plugin_setting_id": 241324,
 *       "mashup_id": null,
 *       "visible": true,
 *       "rendered_at": "2026-02-09T01:27:58.423Z",
 *       "row_order": 2146435072,
 *       "created_at": "2026-02-09T15:15:47.444Z",
 *       "updated_at": "2026-02-09T15:15:47.444Z",
 *       "mirror": false,
 *       "plugin_setting": {
 *         "id": 241324,
 *         "name": "Kung Fu Panda Quotes",
 *         "plugin_id": 37
 *       }
 *     }
 *   ]
 * }
 * ```
 *
 * @property data List of playlist items for a device
 */
@Serializable
data class PlaylistItemsResponse(
    @SerialName("data")
    val data: List<PlaylistItem>,
)
