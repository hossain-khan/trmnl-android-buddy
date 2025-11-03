package ink.trmnl.android.buddy.ui.devicecatalog

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
            name.startsWith("seeed_") -> DeviceKind.SEEED_STUDIO
            name.startsWith("kobo_") -> DeviceKind.KOBO
            else -> DeviceKind.BYOD
        }
