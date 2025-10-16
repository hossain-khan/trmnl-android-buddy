package ink.trmnl.android.buddy.data.preferences

/**
 * Data class representing user preferences stored in DataStore.
 */
data class UserPreferences(
    val apiToken: String? = null,
    val isOnboardingCompleted: Boolean = false,
    val isBatteryTrackingEnabled: Boolean = true,
)
