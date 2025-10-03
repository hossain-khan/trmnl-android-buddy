package ink.trmnl.android.buddy.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

/**
 * Unit tests for [PrivacyUtils].
 */
class PrivacyUtilsTest {
    @Test
    fun `obfuscateDeviceId with standard device ID shows first char and last two chars`() {
        val result = PrivacyUtils.obfuscateDeviceId("ABC123")

        assertThat(result).isEqualTo("A•••23")
    }

    @Test
    fun `obfuscateDeviceId with longer device ID obfuscates middle correctly`() {
        val result = PrivacyUtils.obfuscateDeviceId("DEVICE12345")

        assertThat(result).isEqualTo("D••••••••45")
    }

    @Test
    fun `obfuscateDeviceId with short device ID returns unchanged`() {
        // Device IDs with 3 or fewer characters are returned as-is
        val result1 = PrivacyUtils.obfuscateDeviceId("ABC")
        val result2 = PrivacyUtils.obfuscateDeviceId("AB")
        val result3 = PrivacyUtils.obfuscateDeviceId("A")

        assertThat(result1).isEqualTo("ABC")
        assertThat(result2).isEqualTo("AB")
        assertThat(result3).isEqualTo("A")
    }

    @Test
    fun `obfuscateDeviceId with exactly 4 characters shows first and last two`() {
        val result = PrivacyUtils.obfuscateDeviceId("ABCD")

        assertThat(result).isEqualTo("A•CD")
    }

    @Test
    fun `obfuscateDeviceId with alphanumeric ID preserves format`() {
        val result = PrivacyUtils.obfuscateDeviceId("A1B2C3")

        assertThat(result).isEqualTo("A•••C3")
    }

    @Test
    fun `obfuscateMacAddress with standard MAC address shows first and last segments`() {
        val result = PrivacyUtils.obfuscateMacAddress("AB:CD:EF:12:34:56")

        assertThat(result).isEqualTo("AB:••:••:••:••:56")
    }

    @Test
    fun `obfuscateMacAddress with different MAC address formats`() {
        // Standard 6-segment MAC
        val result1 = PrivacyUtils.obfuscateMacAddress("00:11:22:33:44:55")
        assertThat(result1).isEqualTo("00:••:••:••:••:55")

        // 4-segment MAC
        val result2 = PrivacyUtils.obfuscateMacAddress("AA:BB:CC:DD")
        assertThat(result2).isEqualTo("AA:••:••:DD")

        // 3-segment MAC (minimum for standard format)
        val result3 = PrivacyUtils.obfuscateMacAddress("11:22:33")
        assertThat(result3).isEqualTo("11:••:33")
    }

    @Test
    fun `obfuscateMacAddress with non-standard format obfuscates middle chars`() {
        // Single segment (no colons)
        val result1 = PrivacyUtils.obfuscateMacAddress("ABCDEF")
        assertThat(result1).isEqualTo("AB••EF")

        // Two segments only - treated as non-standard, obfuscates middle
        val result2 = PrivacyUtils.obfuscateMacAddress("AB:CD")
        assertThat(result2).isEqualTo("AB•CD")
    }

    @Test
    fun `obfuscateMacAddress with very short address returns unchanged`() {
        val result1 = PrivacyUtils.obfuscateMacAddress("ABC")
        val result2 = PrivacyUtils.obfuscateMacAddress("AB")
        val result3 = PrivacyUtils.obfuscateMacAddress("A")

        assertThat(result1).isEqualTo("ABC")
        assertThat(result2).isEqualTo("AB")
        assertThat(result3).isEqualTo("A")
    }

    @Test
    fun `obfuscateMacAddress with exactly 4 characters`() {
        val result = PrivacyUtils.obfuscateMacAddress("ABCD")

        assertThat(result).isEqualTo("ABCD")
    }

    @Test
    fun `obfuscateMacAddress with mixed case preserves case in visible parts`() {
        val result = PrivacyUtils.obfuscateMacAddress("aB:Cd:EF:12:34:56")

        assertThat(result).isEqualTo("aB:••:••:••:••:56")
    }

    @Test
    fun `obfuscateDeviceId with empty string returns empty`() {
        val result = PrivacyUtils.obfuscateDeviceId("")

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `obfuscateMacAddress with empty string returns empty`() {
        val result = PrivacyUtils.obfuscateMacAddress("")

        assertThat(result).isEqualTo("")
    }
}
