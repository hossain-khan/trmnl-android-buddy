package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Playlist item that can be displayed on a TRMNL device.
 *
 * Represents a single content item in a device's rotation playlist. Each item can be either
 * a plugin setting or a mashup (collection of plugins). Items can be enabled/disabled via
 * the [visible] property and are rendered in order based on [rowOrder].
 *
 * @property id Unique identifier for this playlist item
 * @property deviceId ID of the device this playlist item belongs to
 * @property pluginSettingId ID of the plugin setting to display (null for mashups)
 * @property mashupId ID of the mashup to display (null for plugin settings)
 * @property visible Whether this item is currently enabled in the rotation
 * @property renderedAt ISO 8601 timestamp of when this item was last displayed (null if never rendered)
 * @property rowOrder Numeric value determining display order (lower values display first)
 * @property createdAt ISO 8601 timestamp of when this item was created
 * @property updatedAt ISO 8601 timestamp of when this item was last updated
 * @property mirror Whether to mirror/flip the display output
 * @property pluginSetting Detailed information about the plugin setting (null for mashups)
 */
@Serializable
data class PlaylistItem(
    @SerialName("id")
    val id: Int,
    @SerialName("device_id")
    val deviceId: Int,
    @SerialName("plugin_setting_id")
    val pluginSettingId: Int?,
    @SerialName("mashup_id")
    val mashupId: Int?,
    @SerialName("visible")
    val visible: Boolean,
    @SerialName("rendered_at")
    val renderedAt: String?,
    @SerialName("row_order")
    val rowOrder: Long,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("mirror")
    val mirror: Boolean,
    @SerialName("plugin_setting")
    val pluginSetting: PluginSetting?,
) {
    /**
     * Get a human-readable display name for this playlist item.
     *
     * Returns the plugin setting name if available, otherwise "Mashup #{id}" for mashup items.
     */
    fun displayName(): String = pluginSetting?.name ?: "Mashup #$mashupId"

    /**
     * Check if this item has never been rendered/displayed.
     */
    fun isNeverRendered(): Boolean = renderedAt == null

    /**
     * Check if this item is a mashup (collection of plugins).
     */
    fun isMashup(): Boolean = mashupId != null
}
