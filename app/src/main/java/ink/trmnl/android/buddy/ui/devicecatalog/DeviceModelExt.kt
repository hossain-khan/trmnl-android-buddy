package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.annotation.DrawableRes
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceModel

/**
 * Extension property to determine the device kind/type for filtering and display.
 *
 * Maps device models to their appropriate [DeviceKind] category based on
 * the device's kind property and name prefix.
 *
 * @return The [DeviceKind] category this device belongs to
 */
val DeviceModel.deviceKind: DeviceKind
    get() =
        when {
            kind == "trmnl" -> DeviceKind.TRMNL
            kind == "kindle" -> DeviceKind.KINDLE
            kind == "joan" || name.startsWith("joan_") -> DeviceKind.JOAN
            name.startsWith("seeed_") -> DeviceKind.SEEED_STUDIO
            name.startsWith("kobo_") -> DeviceKind.KOBO
            name.contains("boox") || name == "palma" -> DeviceKind.BOOX
            name.startsWith("inkplate_") -> DeviceKind.INKPLATE
            else -> DeviceKind.BYOD
        }

/**
 * Returns the brand logo drawable resource for this [DeviceKind], or `null` for generic BYOD devices.
 */
@get:DrawableRes
internal val DeviceKind.logoResource: Int?
    get() =
        when (this) {
            DeviceKind.TRMNL -> R.drawable.trmnl_logo_brand_orange
            DeviceKind.KINDLE -> R.drawable.amazon_kindle_logo
            DeviceKind.SEEED_STUDIO -> R.drawable.seed_studio_color_logo
            DeviceKind.KOBO -> R.drawable.kobo_logo
            DeviceKind.BOOX -> R.drawable.boox_logo_vector
            DeviceKind.JOAN -> R.drawable.joan_logo
            DeviceKind.INKPLATE -> R.drawable.soldered_inkplate_logo
            DeviceKind.BYOD -> null
        }
