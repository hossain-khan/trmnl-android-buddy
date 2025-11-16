package ink.trmnl.android.buddy.fakes

import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [UserPreferencesRepository] for testing.
 *
 * This fake provides a working in-memory implementation suitable for unit tests,
 * following the project's testing guidelines of using fakes instead of mocks.
 *
 * The fake exposes some properties for test assertions (e.g., [batteryTrackingEnabled],
 * [lowBatteryNotificationEnabled], [lowBatteryThreshold], [wasCleared], [savedToken],
 * [onboardingCompleted]) to verify that the repository methods were called with expected values.
 *
 * @param initialPreferences The initial user preferences to start with. Defaults to [UserPreferences] with default values.
 * @param shouldThrowOnSave If true, saveApiToken will throw an exception. Useful for testing error handling.
 */
class FakeUserPreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences(),
    private val shouldThrowOnSave: Boolean = false,
) : UserPreferencesRepository {
    /**
     * Test-visible property to verify battery tracking enabled state.
     */
    var batteryTrackingEnabled = initialPreferences.isBatteryTrackingEnabled
        private set

    /**
     * Test-visible property to verify low battery notification enabled state.
     */
    var lowBatteryNotificationEnabled = initialPreferences.isLowBatteryNotificationEnabled
        private set

    /**
     * Test-visible property to verify low battery threshold value.
     */
    var lowBatteryThreshold = initialPreferences.lowBatteryThresholdPercent
        private set

    /**
     * Test-visible property to verify that clearAll() was called.
     */
    var wasCleared = false
        private set

    /**
     * Test-visible property to verify the last saved API token.
     */
    var savedToken: String? = initialPreferences.apiToken
        private set

    /**
     * Test-visible property to verify onboarding completed state.
     */
    var onboardingCompleted = initialPreferences.isOnboardingCompleted
        private set

    /**
     * Test-visible property to verify security enabled state.
     */
    var securityEnabled = initialPreferences.isSecurityEnabled
        private set

    private val _userPreferencesFlow = MutableStateFlow(initialPreferences)

    override val userPreferencesFlow = _userPreferencesFlow

    override suspend fun saveApiToken(token: String) {
        if (shouldThrowOnSave) {
            throw Exception("Test exception")
        }
        savedToken = token
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = token)
    }

    override suspend fun clearApiToken() {
        savedToken = null
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = null)
    }

    override suspend fun setOnboardingCompleted() {
        onboardingCompleted = true
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isOnboardingCompleted = true)
    }

    override suspend fun setBatteryTrackingEnabled(enabled: Boolean) {
        batteryTrackingEnabled = enabled
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isBatteryTrackingEnabled = enabled)
    }

    override suspend fun setLowBatteryNotificationEnabled(enabled: Boolean) {
        lowBatteryNotificationEnabled = enabled
        _userPreferencesFlow.value =
            _userPreferencesFlow.value.copy(isLowBatteryNotificationEnabled = enabled)
    }

    override suspend fun setLowBatteryThreshold(percent: Int) {
        lowBatteryThreshold = percent
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(lowBatteryThresholdPercent = percent)
    }

    override suspend fun setRssFeedContentEnabled(enabled: Boolean) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isRssFeedContentEnabled = enabled)
    }

    override suspend fun setRssFeedContentNotificationEnabled(enabled: Boolean) {
        _userPreferencesFlow.value =
            _userPreferencesFlow.value.copy(isRssFeedContentNotificationEnabled = enabled)
    }

    /**
     * Test helper to easily set RSS feed content notification enabled state.
     * Useful for tests that need to verify notification behavior.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        _userPreferencesFlow.value =
            _userPreferencesFlow.value.copy(isRssFeedContentNotificationEnabled = enabled)
    }

    override suspend fun setAnnouncementAuthBannerDismissed(dismissed: Boolean) {
        _userPreferencesFlow.value =
            _userPreferencesFlow.value.copy(isAnnouncementAuthBannerDismissed = dismissed)
    }

    override suspend fun setSecurityEnabled(enabled: Boolean) {
        securityEnabled = enabled
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isSecurityEnabled = enabled)
    }

    override suspend fun clearAll() {
        wasCleared = true
        savedToken = null
        onboardingCompleted = false
        securityEnabled = false
        _userPreferencesFlow.value = UserPreferences()
        batteryTrackingEnabled = true
        lowBatteryNotificationEnabled = false
        lowBatteryThreshold = 20
    }
}
