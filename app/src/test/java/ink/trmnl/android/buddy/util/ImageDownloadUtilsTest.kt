package ink.trmnl.android.buddy.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class ImageDownloadUtilsTest {
    @Test
    fun `sanitizeFileName removes forward slash`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device/Name")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes backslash`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device\\Name")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes colon`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device:Name")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes asterisk`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device*Name")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes question mark`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device?Name")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes double quotes`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device\"Name")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes single quotes`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device'Name")).isEqualTo("DeviceName")
        assertThat(ImageDownloadUtils.sanitizeFileName("John's Device")).isEqualTo("Johns_Device")
    }

    @Test
    fun `sanitizeFileName removes angle brackets`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device<Name>")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes pipe character`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device|Name")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes parentheses`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device(Name)")).isEqualTo("DeviceName")
        assertThat(ImageDownloadUtils.sanitizeFileName("Device (v1.0)")).isEqualTo("Device_v1.0")
    }

    @Test
    fun `sanitizeFileName removes square brackets`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device[Name]")).isEqualTo("DeviceName")
        assertThat(ImageDownloadUtils.sanitizeFileName("Device [2024]")).isEqualTo("Device_2024")
    }

    @Test
    fun `sanitizeFileName removes all special characters together`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device/\\:*?\"'<>|()[]Name"))
            .isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName replaces single space with underscore`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device Name")).isEqualTo("Device_Name")
    }

    @Test
    fun `sanitizeFileName replaces multiple spaces with single underscore`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device   Name")).isEqualTo("Device_Name")
    }

    @Test
    fun `sanitizeFileName replaces tabs and newlines with underscore`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device\t\nName")).isEqualTo("Device_Name")
    }

    @Test
    fun `sanitizeFileName removes leading dots`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("...DeviceName")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes trailing dots`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("DeviceName...")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes leading underscores`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("___DeviceName")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes trailing underscores`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("DeviceName___")).isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName removes leading and trailing dots and underscores`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("._DeviceName_."))
            .isEqualTo("DeviceName")
    }

    @Test
    fun `sanitizeFileName returns default when input is empty`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("")).isEqualTo("TRMNL_Display")
    }

    @Test
    fun `sanitizeFileName returns default when only special characters`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("/\\:*?\"'<>|")).isEqualTo("TRMNL_Display")
    }

    @Test
    fun `sanitizeFileName returns default when only whitespace`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("   ")).isEqualTo("TRMNL_Display")
    }

    @Test
    fun `sanitizeFileName returns default when only dots and underscores`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("._._._")).isEqualTo("TRMNL_Display")
    }

    @Test
    fun `sanitizeFileName truncates long filenames to 50 characters`() {
        val longName = "A".repeat(60)
        assertThat(ImageDownloadUtils.sanitizeFileName(longName)).isEqualTo("A".repeat(50))
    }

    @Test
    fun `sanitizeFileName preserves filename at exactly 50 characters`() {
        val exactName = "A".repeat(50)
        assertThat(ImageDownloadUtils.sanitizeFileName(exactName)).isEqualTo(exactName)
    }

    @Test
    fun `sanitizeFileName preserves alphanumeric characters`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device123ABC"))
            .isEqualTo("Device123ABC")
    }

    @Test
    fun `sanitizeFileName preserves hyphens and underscores in middle`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device-Name_V2"))
            .isEqualTo("Device-Name_V2")
    }

    @Test
    fun `sanitizeFileName handles real world device name`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Living Room Display"))
            .isEqualTo("Living_Room_Display")
    }

    @Test
    fun `sanitizeFileName handles device name with apostrophe`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("John's Office Display"))
            .isEqualTo("Johns_Office_Display")
    }

    @Test
    fun `sanitizeFileName handles complex real world case`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Bob's <Main> Display: v2.0"))
            .isEqualTo("Bobs_Main_Display_v2.0")
    }

    @Test
    fun `sanitizeFileName handles mixed special characters and spaces`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("My/Device Name (v1.0)"))
            .isEqualTo("MyDevice_Name_v1.0")
    }

    @Test
    fun `sanitizeFileName preserves dots in middle of filename`() {
        assertThat(ImageDownloadUtils.sanitizeFileName("Device.v1.0"))
            .isEqualTo("Device.v1.0")
    }

    @Test
    fun `sanitizeFileName handles Unicode characters`() {
        // Unicode characters should be preserved
        assertThat(ImageDownloadUtils.sanitizeFileName("Display 📺")).isEqualTo("Display_📺")
        assertThat(ImageDownloadUtils.sanitizeFileName("Café Display")).isEqualTo("Café_Display")
    }
}
