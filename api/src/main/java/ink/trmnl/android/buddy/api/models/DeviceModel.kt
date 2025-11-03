package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a TRMNL device model with its specifications.
 *
 * Device models include official TRMNL devices, Amazon Kindle e-readers,
 * and BYOD (Bring Your Own Device) compatible e-ink displays.
 */
@Serializable
data class DeviceModel(
    val name: String,
    val label: String,
    val description: String,
    val width: Int,
    val height: Int,
    val colors: Int,
    @SerialName("bit_depth")
    val bitDepth: Int,
    @SerialName("scale_factor")
    val scaleFactor: Double,
    val rotation: Int,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("offset_x")
    val offsetX: Int,
    @SerialName("offset_y")
    val offsetY: Int,
    @SerialName("published_at")
    val publishedAt: String,
    val kind: String, // "trmnl", "kindle", "byod"
    @SerialName("palette_ids")
    val paletteIds: List<String>,
) {
    /**
     * Returns a formatted string with device specifications for display.
     * Format: "WIDTHxHEIGHT • COLORS colors • BIT_DEPTH-bit"
     */
    fun getSpecsSummary(): String = "$width×$height • $colors colors • $bitDepth-bit"

    /**
     * Returns the device kind as a [DeviceKind] enum.
     */
    fun getDeviceKind(): DeviceKind? =
        when (kind.lowercase()) {
            "trmnl" -> DeviceKind.TRMNL
            "kindle" -> DeviceKind.KINDLE
            "byod" -> DeviceKind.BYOD
            else -> null
        }
}

/**
 * Device category types.
 */
enum class DeviceKind(val displayName: String) {
    TRMNL("TRMNL"),
    KINDLE("Kindle"),
    BYOD("BYOD"),
}
