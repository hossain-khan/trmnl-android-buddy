package ink.trmnl.android.buddy.domain.models

/**
 * UI-optimized domain model for playlist items.
 *
 * This model represents a playlist item specifically tailored for UI consumption, decoupled
 * from API serialization concerns. It contains only the fields needed for display and
 * includes pre-computed values that would otherwise be calculated in the UI layer.
 *
 * **Design Rationale:**
 * - **Separation of Concerns**: API models handle serialization, domain models handle business logic
 * - **UI Optimization**: Contains only fields actually displayed in the UI
 * - **Computed Fields**: Pre-calculated values (displayName, isMashup) for efficient rendering
 * - **Type Safety**: Simple types without nullable defensive defaults
 * - **Testing**: Easy to construct and test without API dependencies
 *
 * @property id Unique identifier for this playlist item
 * @property deviceId ID of the device this playlist item belongs to
 * @property displayName Human-readable name for display (plugin name or "Mashup #123")
 * @property isVisible Whether this item is currently enabled in the device's rotation
 * @property isMashup Whether this item is a mashup (collection of plugins) vs single plugin
 * @property isNeverRendered Whether this item has never been displayed on the device
 * @property renderedAt ISO 8601 timestamp of last rendering (null if never rendered)
 * @property rowOrder Numeric value determining display order (lower values display first)
 * @property pluginName Name of the plugin if this is a plugin setting (null for mashups)
 * @property mashupId ID of the mashup if this is a mashup item (null for plugin settings)
 *
 * @see ink.trmnl.android.buddy.api.models.PlaylistItem API model that this is derived from
 */
data class PlaylistItemUi(
    val id: Int,
    val deviceId: Int,
    val displayName: String,
    val isVisible: Boolean,
    val isMashup: Boolean,
    val isNeverRendered: Boolean,
    val renderedAt: String?,
    val rowOrder: Long,
    val pluginName: String?,
    val mashupId: Int?,
)
