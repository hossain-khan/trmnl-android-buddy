package ink.trmnl.android.buddy.util

import kotlin.math.abs

/**
 * TRMNL refresh rate options (in minutes) with compact display labels for device list.
 *
 * This list matches the official TRMNL platform options.
 * Format: Pair(minutes, compact_label)
 */
private val TRMNL_REFRESH_RATE_OPTIONS =
    listOf(
        5 to "5m",
        10 to "10m",
        15 to "15m",
        30 to "30m",
        45 to "45m",
        60 to "1h",
        90 to "1.5h",
        120 to "2h",
        240 to "4h",
        360 to "6h",
        480 to "8h",
        720 to "12h",
        1440 to "24h",
    )

/**
 * Formats refresh rate in seconds to a human-readable compact string.
 *
 * Uses closest-match logic to map to official TRMNL refresh rate options,
 * ensuring consistency between device list and device details screens.
 *
 * Examples:
 * - 300s (5 mins) → "5m"
 * - 912s (15.2 mins) → "15m" (closest to 15 mins)
 * - 3600s (60 mins) → "1h"
 * - 6812s (113.5 mins) → "2h" (closest to 120 mins)
 */
fun formatRefreshRate(seconds: Int): String {
    val minutes = seconds / 60

    // Find the closest predefined TRMNL option
    val closestOption =
        TRMNL_REFRESH_RATE_OPTIONS
            .minByOrNull { (optionMinutes, _) ->
                abs(optionMinutes - minutes)
            }

    return closestOption?.second ?: "${seconds}s"
}

/**
 * Formats refresh rate explanation for snackbar display.
 */
fun formatRefreshRateExplanation(seconds: Int): String =
    when {
        seconds < 60 -> "This device checks for new screen content every $seconds seconds"
        seconds < 3600 -> {
            val minutes = seconds / 60
            val minuteText = if (minutes == 1) "minute" else "minutes"
            "This device checks for new screen content every $minutes $minuteText"
        }
        else -> {
            val hours = seconds / 3600
            val hourText = if (hours == 1) "hour" else "hours"
            "This device checks for new screen content every $hours $hourText"
        }
    }
