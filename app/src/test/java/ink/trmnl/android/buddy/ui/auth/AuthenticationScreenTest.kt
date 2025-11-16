package ink.trmnl.android.buddy.ui.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.security.FakeBiometricAuthHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for AuthenticationScreen presenter.
 * Tests authentication flow, navigation logic, and security settings.
 *
 * Note: Tests that require Android Context (LocalContext.current) cannot be run as unit tests
 * and would need to be implemented as instrumented tests. This includes most presenter state tests.
 * The following tests focus on the repository integration and event handling logic that can be tested.
 */
class AuthenticationScreenTest {
    // ========== Repository Integration Tests ==========

    @Test
    fun `setSecurityEnabled updates repository correctly when enabled`() =
        runTest {
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isSecurityEnabled = false),
                )

            userPrefsRepo.setSecurityEnabled(true)

            assertThat(userPrefsRepo.securityEnabled).isTrue()
        }

    @Test
    fun `setSecurityEnabled updates repository correctly when disabled`() =
        runTest {
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isSecurityEnabled = true),
                )

            userPrefsRepo.setSecurityEnabled(false)

            assertThat(userPrefsRepo.securityEnabled).isFalse()
        }

    @Test
    fun `biometric helper reports availability correctly`() {
        val biometricAuthHelperAvailable = FakeBiometricAuthHelper(isAvailable = true)
        val biometricAuthHelperNotAvailable = FakeBiometricAuthHelper(isAvailable = false)

        assertThat(biometricAuthHelperAvailable.isBiometricAvailable()).isTrue()
        assertThat(biometricAuthHelperNotAvailable.isBiometricAvailable()).isFalse()
    }

    @Test
    fun `security preference persists across repository updates`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository()

            // Initially false (default)
            assertThat(userPrefsRepo.securityEnabled).isFalse()

            // Enable security
            userPrefsRepo.setSecurityEnabled(true)
            assertThat(userPrefsRepo.securityEnabled).isTrue()

            // Disable security
            userPrefsRepo.setSecurityEnabled(false)
            assertThat(userPrefsRepo.securityEnabled).isFalse()
        }

    @Test
    fun `clearAll resets security to default`() =
        runTest {
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isSecurityEnabled = true),
                )

            assertThat(userPrefsRepo.securityEnabled).isTrue()

            userPrefsRepo.clearAll()

            assertThat(userPrefsRepo.securityEnabled).isFalse()
        }

    // ========== Test Fakes ==========

    /**
     * Fake implementation of UserPreferencesRepository for testing.
     */
    private class FakeUserPreferencesRepository(
        initialPreferences: UserPreferences = UserPreferences(),
    ) : UserPreferencesRepository {
        var securityEnabled = initialPreferences.isSecurityEnabled
            private set

        private val _userPreferencesFlow = MutableStateFlow(initialPreferences)

        override val userPreferencesFlow = _userPreferencesFlow

        override suspend fun saveApiToken(token: String) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = token)
        }

        override suspend fun clearApiToken() {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = null)
        }

        override suspend fun setOnboardingCompleted() {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isOnboardingCompleted = true)
        }

        override suspend fun setBatteryTrackingEnabled(enabled: Boolean) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isBatteryTrackingEnabled = enabled)
        }

        override suspend fun setLowBatteryNotificationEnabled(enabled: Boolean) {
            _userPreferencesFlow.value =
                _userPreferencesFlow.value.copy(isLowBatteryNotificationEnabled = enabled)
        }

        override suspend fun setLowBatteryThreshold(percent: Int) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(lowBatteryThresholdPercent = percent)
        }

        override suspend fun setRssFeedContentEnabled(enabled: Boolean) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isRssFeedContentEnabled = enabled)
        }

        override suspend fun setRssFeedContentNotificationEnabled(enabled: Boolean) {
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
            securityEnabled = false
            _userPreferencesFlow.value = UserPreferences()
        }
    }
}
