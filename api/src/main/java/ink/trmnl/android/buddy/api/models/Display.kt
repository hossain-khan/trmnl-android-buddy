package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the current display content shown on a TRMNL device.
 *
 * This model contains information about what is currently being displayed
 * on the e-ink screen, based on the `/display/current` Device API endpoint.
 *
 * Note: The Device API endpoint returns the Display object directly (not wrapped in a "data" field).
 *
 * Example JSON response:
 * ```json
 * {
 *   "status": 200,
 *   "refresh_rate": 300,
 *   "image_url": "https://trmnl.com/images/setup/setup-logo.bmp",
 *   "filename": "setup-logo.bmp",
 *   "rendered_at": "2023-01-01T00:00:00Z"
 * }
 * ```
 */
@Serializable
data class Display(
    /**
     * HTTP status code of the response.
     */
    @SerialName("status")
    val status: Int,
    /**
     * Refresh rate in seconds.
     * How often the device checks for new content.
     */
    @SerialName("refresh_rate")
    val refreshRate: Int,
    /**
     * URL to the rendered display image.
     * This is the actual image shown on the e-ink screen.
     * Can be null if no image is available.
     */
    @SerialName("image_url")
    val imageUrl: String? = null,
    /**
     * Filename of the display image.
     * Can be null if no image is available.
     */
    @SerialName("filename")
    val filename: String? = null,
    /**
     * Timestamp when the display was last rendered (ISO 8601 format).
     * Can be null if the display hasn't been rendered yet.
     */
    @SerialName("rendered_at")
    val renderedAt: String? = null,
)
