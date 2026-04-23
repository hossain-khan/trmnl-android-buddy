package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plugin setting information associated with a playlist item.
 *
 * Represents a configured plugin instance that can be displayed on a TRMNL device.
 * Each plugin setting has a unique identifier, user-defined name, and plugin type.
 *
 * NOTE: The API may return an empty object `{}` for `plugin_setting` when the fields are not
 * populated, so all fields are nullable with defaults to avoid deserialization failures.
 *
 * @property id Unique identifier for this plugin setting (null if not populated by API)
 * @property name User-defined name for the plugin setting (e.g., "Morning Weather", "Family Photos")
 * @property pluginId Identifier for the plugin type (e.g., 28 for Weather, 37 for Custom Content)
 */
@Serializable
data class PluginSetting(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("plugin_id")
    val pluginId: Int? = null,
)
