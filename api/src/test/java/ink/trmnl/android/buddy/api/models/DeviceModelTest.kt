package ink.trmnl.android.buddy.api.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

/**
 * Unit tests for Device model helper methods.
 *
 * Tests the battery and WiFi status helper methods to ensure
 * they correctly identify device health conditions.
 */
class DeviceModelTest {
    @Test
    fun `isBatteryHealthy returns true for battery above 50 percent`() {
        // Given: Device with 60% battery
        val device = createTestDevice(percentCharged = 60.0)

        // When: Check if battery is healthy
        val isHealthy = device.isBatteryHealthy()

        // Then: Battery is healthy
        assertThat(isHealthy).isTrue()
    }

    @Test
    fun `isBatteryHealthy returns true for battery exactly at 50 percent`() {
        // Given: Device with exactly 50% battery
        val device = createTestDevice(percentCharged = 50.0)

        // When: Check if battery is healthy
        val isHealthy = device.isBatteryHealthy()

        // Then: Battery is healthy
        assertThat(isHealthy).isTrue()
    }

    @Test
    fun `isBatteryHealthy returns false for battery below 50 percent`() {
        // Given: Device with 45% battery
        val device = createTestDevice(percentCharged = 45.0)

        // When: Check if battery is healthy
        val isHealthy = device.isBatteryHealthy()

        // Then: Battery is not healthy
        assertThat(isHealthy).isFalse()
    }

    @Test
    fun `isWifiStrong returns true for WiFi above 70 percent`() {
        // Given: Device with 80% WiFi strength
        val device = createTestDevice(wifiStrength = 80.0)

        // When: Check if WiFi is strong
        val isStrong = device.isWifiStrong()

        // Then: WiFi is strong
        assertThat(isStrong).isTrue()
    }

    @Test
    fun `isWifiStrong returns true for WiFi exactly at 70 percent`() {
        // Given: Device with exactly 70% WiFi strength
        val device = createTestDevice(wifiStrength = 70.0)

        // When: Check if WiFi is strong
        val isStrong = device.isWifiStrong()

        // Then: WiFi is strong
        assertThat(isStrong).isTrue()
    }

    @Test
    fun `isWifiStrong returns false for WiFi below 70 percent`() {
        // Given: Device with 65% WiFi strength
        val device = createTestDevice(wifiStrength = 65.0)

        // When: Check if WiFi is strong
        val isStrong = device.isWifiStrong()

        // Then: WiFi is not strong
        assertThat(isStrong).isFalse()
    }

    @Test
    fun `getBatteryStatus returns Excellent for 80 percent and above`() {
        // Given: Devices with various battery levels >= 80%
        val device80 = createTestDevice(percentCharged = 80.0)
        val device90 = createTestDevice(percentCharged = 90.0)
        val device100 = createTestDevice(percentCharged = 100.0)

        // Then: All return "Excellent"
        assertThat(device80.getBatteryStatus()).isEqualTo("Excellent")
        assertThat(device90.getBatteryStatus()).isEqualTo("Excellent")
        assertThat(device100.getBatteryStatus()).isEqualTo("Excellent")
    }

    @Test
    fun `getBatteryStatus returns Good for 50 to 79 percent`() {
        // Given: Devices with battery levels between 50% and 79%
        val device50 = createTestDevice(percentCharged = 50.0)
        val device65 = createTestDevice(percentCharged = 65.0)
        val device79 = createTestDevice(percentCharged = 79.0)

        // Then: All return "Good"
        assertThat(device50.getBatteryStatus()).isEqualTo("Good")
        assertThat(device65.getBatteryStatus()).isEqualTo("Good")
        assertThat(device79.getBatteryStatus()).isEqualTo("Good")
    }

    @Test
    fun `getBatteryStatus returns Fair for 20 to 49 percent`() {
        // Given: Devices with battery levels between 20% and 49%
        val device20 = createTestDevice(percentCharged = 20.0)
        val device35 = createTestDevice(percentCharged = 35.0)
        val device49 = createTestDevice(percentCharged = 49.0)

        // Then: All return "Fair"
        assertThat(device20.getBatteryStatus()).isEqualTo("Fair")
        assertThat(device35.getBatteryStatus()).isEqualTo("Fair")
        assertThat(device49.getBatteryStatus()).isEqualTo("Fair")
    }

    @Test
    fun `getWifiStatus returns Excellent for 70 percent and above`() {
        // Given: Devices with WiFi strength >= 70%
        val device70 = createTestDevice(wifiStrength = 70.0)
        val device85 = createTestDevice(wifiStrength = 85.0)
        val device100 = createTestDevice(wifiStrength = 100.0)

        // Then: All return "Excellent"
        assertThat(device70.getWifiStatus()).isEqualTo("Excellent")
        assertThat(device85.getWifiStatus()).isEqualTo("Excellent")
        assertThat(device100.getWifiStatus()).isEqualTo("Excellent")
    }

    @Test
    fun `getWifiStatus returns Good for 50 to 69 percent`() {
        // Given: Devices with WiFi strength between 50% and 69%
        val device50 = createTestDevice(wifiStrength = 50.0)
        val device60 = createTestDevice(wifiStrength = 60.0)
        val device69 = createTestDevice(wifiStrength = 69.0)

        // Then: All return "Good"
        assertThat(device50.getWifiStatus()).isEqualTo("Good")
        assertThat(device60.getWifiStatus()).isEqualTo("Good")
        assertThat(device69.getWifiStatus()).isEqualTo("Good")
    }

    @Test
    fun `getWifiStatus returns Fair for 30 to 49 percent`() {
        // Given: Devices with WiFi strength between 30% and 49%
        val device30 = createTestDevice(wifiStrength = 30.0)
        val device40 = createTestDevice(wifiStrength = 40.0)
        val device49 = createTestDevice(wifiStrength = 49.0)

        // Then: All return "Fair"
        assertThat(device30.getWifiStatus()).isEqualTo("Fair")
        assertThat(device40.getWifiStatus()).isEqualTo("Fair")
        assertThat(device49.getWifiStatus()).isEqualTo("Fair")
    }

    @Test
    fun `getBatteryStatus returns Low for below 20 percent`() {
        // Given: Devices with battery levels below 20%
        val device0 = createTestDevice(percentCharged = 0.0)
        val device10 = createTestDevice(percentCharged = 10.0)
        val device19 = createTestDevice(percentCharged = 19.9)

        // Then: All return "Low"
        assertThat(device0.getBatteryStatus()).isEqualTo("Low")
        assertThat(device10.getBatteryStatus()).isEqualTo("Low")
        assertThat(device19.getBatteryStatus()).isEqualTo("Low")
    }

    @Test
    fun `getWifiStatus returns Weak for below 30 percent`() {
        // Given: Devices with WiFi strength below 30%
        val device0 = createTestDevice(wifiStrength = 0.0)
        val device15 = createTestDevice(wifiStrength = 15.0)
        val device29 = createTestDevice(wifiStrength = 29.9)

        // Then: All return "Weak"
        assertThat(device0.getWifiStatus()).isEqualTo("Weak")
        assertThat(device15.getWifiStatus()).isEqualTo("Weak")
        assertThat(device29.getWifiStatus()).isEqualTo("Weak")
    }

    @Test
    fun `isBatteryLow returns true for battery below 20 percent`() {
        // Given: Device with 15% battery
        val device = createTestDevice(percentCharged = 15.0)

        // When: Check if battery is low
        val isLow = device.isBatteryLow()

        // Then: Battery is low
        assertThat(isLow).isTrue()
    }

    @Test
    fun `isBatteryLow returns false for battery at 20 percent`() {
        // Given: Device with exactly 20% battery
        val device = createTestDevice(percentCharged = 20.0)

        // When: Check if battery is low
        val isLow = device.isBatteryLow()

        // Then: Battery is not low (threshold is < 20%)
        assertThat(isLow).isFalse()
    }

    @Test
    fun `isBatteryLow returns false for battery above 20 percent`() {
        // Given: Device with 25% battery
        val device = createTestDevice(percentCharged = 25.0)

        // When: Check if battery is low
        val isLow = device.isBatteryLow()

        // Then: Battery is not low
        assertThat(isLow).isFalse()
    }

    @Test
    fun `isWifiWeak returns true for WiFi below 40 percent`() {
        // Given: Device with 30% WiFi strength
        val device = createTestDevice(wifiStrength = 30.0)

        // When: Check if WiFi is weak
        val isWeak = device.isWifiWeak()

        // Then: WiFi is weak
        assertThat(isWeak).isTrue()
    }

    @Test
    fun `isWifiWeak returns false for WiFi at 40 percent`() {
        // Given: Device with exactly 40% WiFi strength
        val device = createTestDevice(wifiStrength = 40.0)

        // When: Check if WiFi is weak
        val isWeak = device.isWifiWeak()

        // Then: WiFi is not weak (threshold is < 40%)
        assertThat(isWeak).isFalse()
    }

    @Test
    fun `isWifiWeak returns false for WiFi above 40 percent`() {
        // Given: Device with 50% WiFi strength
        val device = createTestDevice(wifiStrength = 50.0)

        // When: Check if WiFi is weak
        val isWeak = device.isWifiWeak()

        // Then: WiFi is not weak
        assertThat(isWeak).isFalse()
    }

    /**
     * Helper method to create a test device with customizable properties.
     */
    private fun createTestDevice(
        id: Int = 1,
        name: String = "Test Device",
        friendlyId: String = "TEST",
        macAddress: String = "00:11:22:33:44:55",
        batteryVoltage: Double? = 3.8,
        rssi: Int? = -50,
        percentCharged: Double = 75.0,
        wifiStrength: Double = 80.0,
    ): Device =
        Device(
            id = id,
            name = name,
            friendlyId = friendlyId,
            macAddress = macAddress,
            batteryVoltage = batteryVoltage,
            rssi = rssi,
            percentCharged = percentCharged,
            wifiStrength = wifiStrength,
        )
}
