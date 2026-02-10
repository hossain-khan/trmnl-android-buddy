package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plugin setting information associated with a playlist item.
 *
 * Represents a configured plugin instance that can be displayed on a TRMNL device.
 * Each plugin setting has a unique identifier, user-defined name, and plugin type.
 *
 * @property id Unique identifier for this plugin setting
 * @property name User-defined name for the plugin setting (e.g., "Morning Weather", "Family Photos")
 * @property pluginId Identifier for the plugin type (e.g., 28 for Weather, 37 for Custom Content)
 */
@Serializable
data class PluginSetting(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("plugin_id")
    val pluginId: Int,
)
