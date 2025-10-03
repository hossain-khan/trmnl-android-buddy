package ink.trmnl.android.buddy.util

/**
 * Utility functions for privacy-related operations such as obfuscating sensitive information.
 */
object PrivacyUtils {
    /**
     * Obfuscates a device ID for privacy by showing only the first character and last 2 characters.
     * Example: "ABC123" becomes "A•••23"
     *
     * @param deviceId The device ID to obfuscate
     * @return The obfuscated device ID
     */
    fun obfuscateDeviceId(deviceId: String): String {
        if (deviceId.length <= 3) return deviceId

        val first = deviceId.take(1)
        val last = deviceId.takeLast(2)
        val middle = "•".repeat(deviceId.length - 3)

        return "$first$middle$last"
    }

    /**
     * Obfuscates a MAC address for privacy by showing only the first and last segments.
     * Example: "AB:CD:EF:12:34:56" becomes "AB:••:••:••:••:56"
     *
     * @param macAddress The MAC address to obfuscate
     * @return The obfuscated MAC address
     */
    fun obfuscateMacAddress(macAddress: String): String {
        if (macAddress.length < 4) return macAddress

        val parts = macAddress.split(":")
        if (parts.size <= 2) {
            // If it's not a standard MAC address format, just obfuscate the middle
            return "${macAddress.take(2)}${"•".repeat(macAddress.length - 4)}${macAddress.takeLast(2)}"
        }

        // Standard MAC address format (e.g., "AB:CD:EF:12:34:56")
        // Show first part and last part, obfuscate everything in between with centered bullets
        return "${parts.first()}:${"••:".repeat(parts.size - 2)}${parts.last()}"
    }
}
