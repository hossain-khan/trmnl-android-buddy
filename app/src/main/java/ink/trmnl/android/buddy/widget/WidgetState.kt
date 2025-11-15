package ink.trmnl.android.buddy.widget

import kotlinx.serialization.Serializable

/**
 * State data for the device widget.
 * Represents the current state of a widget instance.
 */
@Serializable
data class WidgetState(
    /**
     * The device ID this widget is displaying.
     * Null if no device has been selected yet.
     */
    val deviceId: Int? = null,
    /**
     * The device name for display purposes.
     */
    val deviceName: String? = null,
    /**
     * The device friendly ID (e.g., "ABC-123").
     */
    val deviceFriendlyId: String? = null,
    /**
     * URL to the current display image shown on the device.
     */
    val imageUrl: String? = null,
    /**
     * Local file path to the cached display image.
     * This is used by Glance to display the image.
     */
    val imagePath: String? = null,
    /**
     * Battery percentage (0.0 to 100.0).
     */
    val batteryPercent: Double? = null,
    /**
     * WiFi strength percentage (0.0 to 100.0).
     */
    val wifiStrength: Double? = null,
    /**
     * Timestamp when the data was last updated.
     */
    val lastUpdated: Long = System.currentTimeMillis(),
    /**
     * Whether the widget is currently loading data.
     */
    val isLoading: Boolean = false,
    /**
     * Error message if data fetching failed.
     */
    val errorMessage: String? = null,
)
