package ink.trmnl.android.buddy.util

/**
 * Formats refresh rate in seconds to a human-readable string.
 */
fun formatRefreshRate(seconds: Int): String =
    when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val minutes = seconds / 60
            "${minutes}m"
        }
        else -> {
            val hours = seconds / 3600
            "${hours}h"
        }
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
