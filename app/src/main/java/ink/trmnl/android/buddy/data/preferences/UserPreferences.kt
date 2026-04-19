package ink.trmnl.android.buddy.data.preferences

/**
 * Data class representing user preferences stored in DataStore.
 *
 * @property apiToken TRMNL User API key (Bearer token)
 * @property isOnboardingCompleted Whether the user has finished the initial setup
 * @property isBatteryTrackingEnabled Whether automatic weekly battery data collection is enabled
 * @property isLowBatteryNotificationEnabled Whether notifications for low battery are enabled
 * @property lowBatteryThresholdPercent Battery percentage threshold for low battery alerts
 * @property isRssFeedContentEnabled Whether RSS feed content (announcements/blog posts) is enabled
 * @property isRssFeedContentNotificationEnabled Whether notifications for new RSS content are enabled
 * @property isAnnouncementAuthBannerDismissed Whether the announcement authentication banner has been hidden
 * @property isSecurityEnabled Whether biometric/device credential authentication is enabled
 * @property isShowRecipeHealthCardEnabled Whether the Recipe Health Card is visible on the devices list
 */
data class UserPreferences(
    val apiToken: String? = null,
    val isOnboardingCompleted: Boolean = false,
    val isBatteryTrackingEnabled: Boolean = true,
    val isLowBatteryNotificationEnabled: Boolean = false,
    val lowBatteryThresholdPercent: Int = DEFAULT_LOW_BATTERY_THRESHOLD,
    val isRssFeedContentEnabled: Boolean = false,
    val isRssFeedContentNotificationEnabled: Boolean = false,
    val isAnnouncementAuthBannerDismissed: Boolean = false,
    val isSecurityEnabled: Boolean = false,
    val isShowRecipeHealthCardEnabled: Boolean = true,
) {
    companion object {
        /**
         * Default battery threshold percentage for low battery notifications.
         * Alerts will be triggered when device battery falls below this value.
         */
        const val DEFAULT_LOW_BATTERY_THRESHOLD = 20
    }
}
