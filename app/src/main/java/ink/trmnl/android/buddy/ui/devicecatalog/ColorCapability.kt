package ink.trmnl.android.buddy.ui.devicecatalog

import ink.trmnl.android.buddy.api.models.DeviceModel

/**
 * Categorizes devices by their color display capabilities.
 *
 * Used to provide visual indicators in the device catalog to help users
 * quickly identify device characteristics at a glance.
 *
 * Categories are determined by the number of colors a device supports:
 * - Full Color: Devices with more than 1000 colors (e.g., Tidbyt with 16M colors)
 * - Grayscale: Devices with 16-256 colors (e.g., Kindle devices, Palma)
 * - Multi-tone: Devices with 4-16 colors (e.g., TRMNL X, Kobo devices)
 * - Monochrome: Devices with 2-3 colors (e.g., TRMNL OG, Inky Impression)
 */
enum class ColorCapability {
    /**
     * Full color displays with more than 1000 colors.
     * Examples: Tidbyt (16,777,216 colors)
     */
    FULL_COLOR,

    /**
     * Grayscale displays with 16-256 color gradations.
     * Examples: Kindle devices (256 colors), Palma (256 colors)
     */
    GRAYSCALE,

    /**
     * Multi-tone displays with 4-16 colors.
     * Examples: TRMNL X (16 colors), Kobo devices (16 colors), M5PaperS3 (16 colors)
     */
    MULTI_TONE,

    /**
     * Monochrome displays with 2-3 colors (black/white or black/white/red).
     * Examples: TRMNL OG (2 colors), Inky Impression (2 colors), Waveshare B/W (2 colors)
     */
    MONOCHROME,
}

/**
 * Extension property to determine the color capability category of a device.
 *
 * Categorizes devices based on the number of colors they support.
 *
 * @return The [ColorCapability] category this device belongs to
 */
val DeviceModel.colorCapability: ColorCapability
    get() =
        when {
            colors > 1000 -> ColorCapability.FULL_COLOR
            colors >= 16 -> ColorCapability.GRAYSCALE
            colors >= 4 -> ColorCapability.MULTI_TONE
            else -> ColorCapability.MONOCHROME
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
