package ink.trmnl.android.buddy.ui.devicecatalog

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceModel
import org.junit.Test

/**
 * Unit tests for [DeviceModel.deviceKind] and [DeviceKind.logoResource].
 */
class DeviceModelExtTest {
    @Test
    fun `deviceKind maps joan kind and joan prefix to JOAN`() {
        val byKind = createDeviceModel(name = "custom_visionect", kind = "joan")
        val byName = createDeviceModel(name = "joan_6", kind = "byod")

        assertThat(byKind.deviceKind).isEqualTo(DeviceKind.JOAN)
        assertThat(byName.deviceKind).isEqualTo(DeviceKind.JOAN)
    }

    @Test
    fun `deviceKind maps inkplate prefix to INKPLATE`() {
        val inkplate = createDeviceModel(name = "inkplate_10", kind = "byod")

        assertThat(inkplate.deviceKind).isEqualTo(DeviceKind.INKPLATE)
    }

    @Test
    fun `deviceKind maps existing brands and falls back to BYOD`() {
        assertThat(createDeviceModel(name = "og_png", kind = "trmnl").deviceKind).isEqualTo(DeviceKind.TRMNL)
        assertThat(createDeviceModel(name = "amazon_kindle_2024", kind = "kindle").deviceKind).isEqualTo(DeviceKind.KINDLE)
        assertThat(createDeviceModel(name = "boox_palma", kind = "byod").deviceKind).isEqualTo(DeviceKind.BOOX)
        assertThat(createDeviceModel(name = "waveshare_75", kind = "byod").deviceKind).isEqualTo(DeviceKind.WAVESHARE)
        assertThat(createDeviceModel(name = "generic_epaper_diy", kind = "byod").deviceKind).isEqualTo(DeviceKind.BYOD)
    }

    @Test
    fun `logoResource returns expected drawables for each DeviceKind`() {
        assertThat(DeviceKind.TRMNL.logoResource).isEqualTo(R.drawable.trmnl_logo_brand_orange)
        assertThat(DeviceKind.KINDLE.logoResource).isEqualTo(R.drawable.amazon_kindle_logo)
        assertThat(DeviceKind.SEEED_STUDIO.logoResource).isEqualTo(R.drawable.seed_studio_color_logo)
        assertThat(DeviceKind.KOBO.logoResource).isEqualTo(R.drawable.kobo_logo)
        assertThat(DeviceKind.BOOX.logoResource).isEqualTo(R.drawable.boox_logo_vector)
        assertThat(DeviceKind.JOAN.logoResource).isEqualTo(R.drawable.joan_logo)
        assertThat(DeviceKind.INKPLATE.logoResource).isEqualTo(R.drawable.soldered_inkplate_logo)
        assertThat(DeviceKind.WAVESHARE.logoResource).isEqualTo(R.drawable.waveshare_logo)
        assertThat(DeviceKind.BYOD.logoResource).isNull()
    }

    @Test
    fun `displayName returns human-readable labels for each DeviceKind`() {
        assertThat(DeviceKind.TRMNL.displayName).isEqualTo("TRMNL")
        assertThat(DeviceKind.KINDLE.displayName).isEqualTo("Kindle")
        assertThat(DeviceKind.SEEED_STUDIO.displayName).isEqualTo("Seeed Studio")
        assertThat(DeviceKind.KOBO.displayName).isEqualTo("Kobo")
        assertThat(DeviceKind.BOOX.displayName).isEqualTo("BOOX")
        assertThat(DeviceKind.JOAN.displayName).isEqualTo("Joan")
        assertThat(DeviceKind.INKPLATE.displayName).isEqualTo("Inkplate")
        assertThat(DeviceKind.WAVESHARE.displayName).isEqualTo("Waveshare")
        assertThat(DeviceKind.BYOD.displayName).isEqualTo("Other BYOD")
    }

    private fun createDeviceModel(
        name: String,
        kind: String,
    ) = DeviceModel(
        name = name,
        label = name,
        description = name,
        width = 800,
        height = 480,
        colors = 2,
        bitDepth = 1,
        scaleFactor = 1.0,
        rotation = 0,
        mimeType = "image/png",
        offsetX = 0,
        offsetY = 0,
        publishedAt = "2024-01-01T00:00:00.000Z",
        kind = kind,
        paletteIds = listOf("bw"),
    )
}
