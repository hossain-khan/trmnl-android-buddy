package ink.trmnl.android.buddy.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ink.trmnl.android.buddy.R

/**
 * Returns the appropriate battery color based on charge percentage.
 * Uses theme-aware colors that work with both light and dark themes.
 */
@Composable
fun getBatteryColor(percentCharged: Double): Color {
    val goodColor = MaterialTheme.colorScheme.tertiary
    val warningColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val criticalColor = MaterialTheme.colorScheme.error

    return when {
        percentCharged >= 60 -> goodColor // Good battery level
        percentCharged >= 30 -> warningColor // Warning level
        else -> criticalColor // Critical level
    }
}

/**
 * Returns the appropriate WiFi color based on signal strength percentage.
 * Uses theme-aware colors that work with both light and dark themes.
 */
@Composable
fun getWifiColor(wifiStrength: Double): Color {
    val strongColor = MaterialTheme.colorScheme.tertiary
    val mediumColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val weakColor = MaterialTheme.colorScheme.error

    return when {
        wifiStrength >= 70 -> strongColor // Strong signal
        wifiStrength >= 40 -> mediumColor // Medium signal
        else -> weakColor // Weak signal
    }
}

/**
 * Returns the appropriate battery icon based on the charge percentage.
 */
fun getBatteryIcon(percentCharged: Double): Int =
    when {
        percentCharged >= 95 -> R.drawable.outline_battery_android_full_24
        percentCharged >= 80 -> R.drawable.outline_battery_android_6_24
        percentCharged >= 65 -> R.drawable.outline_battery_android_5_24
        percentCharged >= 50 -> R.drawable.outline_battery_android_4_24
        percentCharged >= 35 -> R.drawable.outline_battery_android_3_24
        percentCharged >= 20 -> R.drawable.outline_battery_android_2_24
        percentCharged >= 5 -> R.drawable.outline_battery_android_1_24
        else -> R.drawable.outline_battery_android_0_24
    }

/**
 * Returns the appropriate WiFi signal icon based on signal strength percentage.
 */
fun getWifiIcon(wifiStrength: Double): Int =
    when {
        wifiStrength >= 75 -> R.drawable.outline_signal_wifi_4_bar_24
        wifiStrength >= 50 -> R.drawable.outline_network_wifi_3_bar_24
        wifiStrength >= 25 -> R.drawable.outline_network_wifi_2_bar_24
        else -> R.drawable.outline_network_wifi_1_bar_24
    }

/**
 * Checks if a device's battery level is below the specified threshold.
 *
 * @param percentCharged Current battery percentage (0-100)
 * @param thresholdPercent Low battery threshold percentage (0-100)
 * @param isNotificationEnabled Whether low battery notifications are enabled
 * @return true if battery is below threshold and notifications are enabled
 */
fun isLowBatteryAlert(
    percentCharged: Double,
    thresholdPercent: Int,
    isNotificationEnabled: Boolean,
): Boolean = isNotificationEnabled && percentCharged < thresholdPercent
