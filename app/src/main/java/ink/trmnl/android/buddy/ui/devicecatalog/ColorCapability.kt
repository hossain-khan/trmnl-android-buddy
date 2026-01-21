package ink.trmnl.android.buddy.ui.devicecatalog

import ink.trmnl.android.buddy.api.models.DeviceModel

/**
 * Categorizes devices by their color display capabilities.
 *
 * Used to provide visual indicators in the device catalog to help users
 * quickly identify device characteristics at a glance.
 *
 * Categories are determined by the number of colors a device supports:
 * - Full Color: More than 1000 colors (e.g., Tidbyt with 16M colors)
 * - Grayscale: 17-1000 colors (e.g., Kindle devices with 256)
 * - Multi-tone: 4-16 colors (e.g., TRMNL X with 16, Inkplate with 8)
 * - Monochrome: 2-3 colors (e.g., TRMNL OG 1-bit with 2)
 */
enum class ColorCapability {
    /**
     * Full color displays with more than 1000 colors.
     * Examples: Tidbyt (16,777,216 colors)
     */
    FULL_COLOR,

    /**
     * Grayscale displays with 17-1000 colors.
     * Examples: Kindle devices (256 colors), Palma (256 colors)
     */
    GRAYSCALE,

    /**
     * Multi-tone displays with 4-16 colors.
     * Examples: TRMNL X (16 colors), Kobo devices (16 colors), Inkplate 10 (8 colors)
     */
    MULTI_TONE,

    /**
     * Monochrome displays with 2-3 colors (black/white or black/white/red).
     * Examples: TRMNL OG 1-bit (2 colors), Inky Impression (2 colors), Waveshare B/W (2 colors)
     */
    MONOCHROME,
}

/**
 * Extension property to determine the color capability category of a device.
 *
 * Categorizes devices based on the number of colors they support:
 * - Full Color: >1000 colors (e.g., 16,777,216)
 * - Grayscale: 17-1000 colors (e.g., 256)
 * - Multi-tone: 4-16 colors (e.g., 4, 8, 16)
 * - Monochrome: 2-3 colors (e.g., 2, 3)
 *
 * @return The [ColorCapability] category this device belongs to
 */
val DeviceModel.colorCapability: ColorCapability
    get() =
        when {
            colors > 1000 -> ColorCapability.FULL_COLOR // >1000 colors
            colors > 16 -> ColorCapability.GRAYSCALE // 17-1000 colors
            colors >= 4 -> ColorCapability.MULTI_TONE // 4-16 colors
            else -> ColorCapability.MONOCHROME // 2-3 colors
        }

/**
 * Get a human-readable label for the color capability.
 *
 * @return Display label for the color capability
 */
fun ColorCapability.getLabel(): String =
    when (this) {
        ColorCapability.FULL_COLOR -> "Full Color"
        ColorCapability.GRAYSCALE -> "Grayscale"
        ColorCapability.MULTI_TONE -> "Multi-tone"
        ColorCapability.MONOCHROME -> "Monochrome"
    }
