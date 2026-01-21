package ink.trmnl.android.buddy.ui.devicecatalog

import assertk.assertThat
import assertk.assertions.isEqualTo
import ink.trmnl.android.buddy.api.models.DeviceModel
import org.junit.Test

/**
 * Unit tests for ColorCapability categorization logic.
 *
 * Tests verify correct categorization of devices based on their color count:
 * - Full Color: >1000 colors
 * - Grayscale: 16-256 colors
 * - Multi-tone: 4-16 colors
 * - Monochrome: 2-3 colors
 */
class ColorCapabilityTest {
    @Test
    fun `device with 16 million colors is Full Color`() {
        val device = createTestDevice(colors = 16_777_216)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.FULL_COLOR)
    }

    @Test
    fun `device with 2000 colors is Full Color`() {
        val device = createTestDevice(colors = 2000)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.FULL_COLOR)
    }

    @Test
    fun `device with 1001 colors is Full Color`() {
        val device = createTestDevice(colors = 1001)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.FULL_COLOR)
    }

    @Test
    fun `device with 256 colors is Grayscale`() {
        val device = createTestDevice(colors = 256)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.GRAYSCALE)
    }

    @Test
    fun `device with 128 colors is Grayscale`() {
        val device = createTestDevice(colors = 128)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.GRAYSCALE)
    }

    @Test
    fun `device with 16 colors is Grayscale`() {
        val device = createTestDevice(colors = 16)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.GRAYSCALE)
    }

    @Test
    fun `device with 8 colors is Multi-tone`() {
        val device = createTestDevice(colors = 8)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.MULTI_TONE)
    }

    @Test
    fun `device with 4 colors is Multi-tone`() {
        val device = createTestDevice(colors = 4)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.MULTI_TONE)
    }

    @Test
    fun `device with 3 colors is Monochrome`() {
        val device = createTestDevice(colors = 3)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.MONOCHROME)
    }

    @Test
    fun `device with 2 colors is Monochrome`() {
        val device = createTestDevice(colors = 2)

        assertThat(device.colorCapability).isEqualTo(ColorCapability.MONOCHROME)
    }

    @Test
    fun `Full Color label is correct`() {
        assertThat(ColorCapability.FULL_COLOR.getLabel()).isEqualTo("Full Color")
    }

    @Test
    fun `Grayscale label is correct`() {
        assertThat(ColorCapability.GRAYSCALE.getLabel()).isEqualTo("Grayscale")
    }

    @Test
    fun `Multi-tone label is correct`() {
        assertThat(ColorCapability.MULTI_TONE.getLabel()).isEqualTo("Multi-tone")
    }

    @Test
    fun `Monochrome label is correct`() {
        assertThat(ColorCapability.MONOCHROME.getLabel()).isEqualTo("Monochrome")
    }

    /**
     * Helper function to create a test device model with specified colors.
     */
    private fun createTestDevice(colors: Int): DeviceModel =
        DeviceModel(
            name = "test_device",
            label = "Test Device",
            description = "Test device for color capability",
            width = 800,
            height = 480,
            colors = colors,
            bitDepth = 1,
            scaleFactor = 1.0,
            rotation = 0,
            mimeType = "image/png",
            offsetX = 0,
            offsetY = 0,
            publishedAt = "2024-01-01T00:00:00.000Z",
            kind = "test",
            paletteIds = listOf("test"),
        )
}
