package ink.trmnl.android.buddy.data.preferences

/**
 * Data class representing user preferences stored in DataStore.
 */
data class UserPreferences(
    val apiToken: String? = null,
    val isOnboardingCompleted: Boolean = false,
    val isBatteryTrackingEnabled: Boolean = true,
    val isLowBatteryNotificationEnabled: Boolean = false,
    val lowBatteryThresholdPercent: Int = DEFAULT_LOW_BATTERY_THRESHOLD,
    val isRssFeedContentEnabled: Boolean = true,
    val isAnnouncementAuthBannerDismissed: Boolean = false,
) {
    companion object {
        /**
         * Default battery threshold percentage for low battery notifications.
         * Alerts will be triggered when device battery falls below this value.
         */
        const val DEFAULT_LOW_BATTERY_THRESHOLD = 20
    }
}
