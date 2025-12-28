package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TRMNL Device Model.
 *
 * Represents a supported e-ink display device model with its specifications.
 * This includes official TRMNL devices, Amazon Kindle e-readers, and third-party BYOD devices.
 *
 * @property name Unique identifier for the device model (e.g., "og_png", "amazon_kindle_2024")
 * @property label Display name for the device (e.g., "TRMNL OG (1-bit)", "Amazon Kindle 2024")
 * @property description Device description
 * @property width Screen width in pixels
 * @property height Screen height in pixels
 * @property colors Number of colors supported (e.g., 2 for black/white, 256 for grayscale)
 * @property bitDepth Color depth in bits (e.g., 1 for 1-bit, 8 for 8-bit)
 * @property scaleFactor Display scale factor
 * @property rotation Display rotation in degrees (0, 90, 180, 270)
 * @property mimeType Image MIME type (e.g., "image/png")
 * @property offsetX Horizontal offset in pixels
 * @property offsetY Vertical offset in pixels
 * @property publishedAt ISO 8601 timestamp when the model was published (optional, nullable)
 * @property kind Device category: "trmnl" (official), "kindle" (Amazon), or "byod" (third-party)
 * @property paletteIds List of supported color palette IDs
 */
@Serializable
data class DeviceModel(
    @SerialName("name")
    val name: String,
    @SerialName("label")
    val label: String,
    @SerialName("description")
    val description: String,
    @SerialName("width")
    val width: Int,
    @SerialName("height")
    val height: Int,
    @SerialName("colors")
    val colors: Int,
    @SerialName("bit_depth")
    val bitDepth: Int,
    @SerialName("scale_factor")
    val scaleFactor: Double,
    @SerialName("rotation")
    val rotation: Int,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("offset_x")
    val offsetX: Int,
    @SerialName("offset_y")
    val offsetY: Int,
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("kind")
    val kind: String,
    @SerialName("palette_ids")
    val paletteIds: List<String>,
) {
    /**
     * Get device specifications summary for display.
     * Format: "800×480 • 2 colors • 1-bit"
     */
    fun getSpecsSummary(): String = "$width×$height • $colors colors • $bitDepth-bit"
}
