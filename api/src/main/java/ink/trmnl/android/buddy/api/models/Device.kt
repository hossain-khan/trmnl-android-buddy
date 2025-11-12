package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TRMNL Device model.
 *
 * Represents a TRMNL e-ink display device with its current status and metrics.
 *
 * @property id Unique identifier for the device
 * @property name User-defined name for the device
 * @property friendlyId Short, user-friendly device identifier (e.g., "ABC-123")
 * @property macAddress Device MAC address in format "12:34:56:78:9A:BC"
 * @property batteryVoltage Current battery voltage in volts (e.g., 3.7V). Null if not reported.
 * @property rssi WiFi signal strength indicator in dBm (e.g., -70). More negative = weaker signal. Null if not reported.
 * @property percentCharged Battery charge percentage from 0.0 to 100.0
 * @property wifiStrength WiFi signal strength percentage from 0.0 to 100.0 (normalized from RSSI)
 */
@Serializable
data class Device(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("friendly_id")
    val friendlyId: String,
    @SerialName("mac_address")
    val macAddress: String,
    @SerialName("battery_voltage")
    val batteryVoltage: Double?,
    @SerialName("rssi")
    val rssi: Int?,
    @SerialName("percent_charged")
    val percentCharged: Double,
    @SerialName("wifi_strength")
    val wifiStrength: Double,
) {
    /**
     * Check if the device battery is critically low (below 20%).
     */
    fun isBatteryLow(): Boolean = percentCharged < 20.0

    /**
     * Check if the device battery is healthy (above 50%).
     */
    fun isBatteryHealthy(): Boolean = percentCharged >= 50.0

    /**
     * Check if the WiFi signal is weak (below 40%).
     */
    fun isWifiWeak(): Boolean = wifiStrength < 40.0

    /**
     * Check if the WiFi signal is strong (above 70%).
     */
    fun isWifiStrong(): Boolean = wifiStrength >= 70.0

    /**
     * Get battery status as a human-readable string.
     */
    fun getBatteryStatus(): String =
        when {
            percentCharged >= 80.0 -> "Excellent"
            percentCharged >= 50.0 -> "Good"
            percentCharged >= 20.0 -> "Fair"
            else -> "Low"
        }

    /**
     * Get WiFi status as a human-readable string.
     */
    fun getWifiStatus(): String =
        when {
            wifiStrength >= 70.0 -> "Excellent"
            wifiStrength >= 50.0 -> "Good"
            wifiStrength >= 30.0 -> "Fair"
            else -> "Weak"
        }
}
