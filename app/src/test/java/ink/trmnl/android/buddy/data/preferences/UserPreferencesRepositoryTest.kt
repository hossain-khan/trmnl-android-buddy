package ink.trmnl.android.buddy.data.preferences

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [UserPreferencesRepository] interface using a fake implementation.
 *
 * Tests user preferences storage operations without requiring actual DataStore.
 */
class UserPreferencesRepositoryTest {
    @Test
    fun `userPreferencesFlow emits default values initially`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When/Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()

                // Verify default values
                assertThat(prefs.apiToken).isNull()
                assertThat(prefs.isOnboardingCompleted).isFalse()
                assertThat(prefs.isBatteryTrackingEnabled).isTrue()
                assertThat(prefs.isLowBatteryNotificationEnabled).isFalse()
                assertThat(prefs.lowBatteryThresholdPercent).isEqualTo(UserPreferences.DEFAULT_LOW_BATTERY_THRESHOLD)
                assertThat(prefs.isRssFeedContentEnabled).isFalse()
                assertThat(prefs.isRssFeedContentNotificationEnabled).isFalse()
                assertThat(prefs.isAnnouncementAuthBannerDismissed).isFalse()
                assertThat(prefs.isSecurityEnabled).isFalse()
            }
        }

    @Test
    fun `saveApiToken stores and emits token`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.saveApiToken("test-api-token-123")

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.apiToken).isEqualTo("test-api-token-123")
            }
        }

    @Test
    fun `clearApiToken removes token`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()
            repository.saveApiToken("test-token")

            // When
            repository.clearApiToken()

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.apiToken).isNull()
            }
        }

    @Test
    fun `setOnboardingCompleted sets flag to true`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setOnboardingCompleted()

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isOnboardingCompleted).isTrue()
            }
        }

    @Test
    fun `setBatteryTrackingEnabled updates preference`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setBatteryTrackingEnabled(false)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isBatteryTrackingEnabled).isFalse()
            }
        }

    @Test
    fun `setBatteryTrackingEnabled can be set back to true`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()
            repository.setBatteryTrackingEnabled(false)

            // When
            repository.setBatteryTrackingEnabled(true)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isBatteryTrackingEnabled).isTrue()
            }
        }

    @Test
    fun `setLowBatteryNotificationEnabled updates preference`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setLowBatteryNotificationEnabled(true)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isLowBatteryNotificationEnabled).isTrue()
            }
        }

    @Test
    fun `setLowBatteryThreshold updates threshold value`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setLowBatteryThreshold(15)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.lowBatteryThresholdPercent).isEqualTo(15)
            }
        }

    @Test
    fun `setRssFeedContentEnabled updates preference`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setRssFeedContentEnabled(true)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isRssFeedContentEnabled).isTrue()
            }
        }

    @Test
    fun `setRssFeedContentNotificationEnabled updates preference`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setRssFeedContentNotificationEnabled(true)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isRssFeedContentNotificationEnabled).isTrue()
            }
        }

    @Test
    fun `setAnnouncementAuthBannerDismissed updates preference`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setAnnouncementAuthBannerDismissed(true)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isAnnouncementAuthBannerDismissed).isTrue()
            }
        }

    @Test
    fun `setSecurityEnabled updates preference`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setSecurityEnabled(true)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isSecurityEnabled).isTrue()
            }
        }

    @Test
    fun `clearAll removes all preferences`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()
            repository.saveApiToken("test-token")
            repository.setOnboardingCompleted()
            repository.setBatteryTrackingEnabled(false)
            repository.setLowBatteryThreshold(25)

            // When
            repository.clearAll()

            // Then - all should be back to defaults
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.apiToken).isNull()
                assertThat(prefs.isOnboardingCompleted).isFalse()
                assertThat(prefs.isBatteryTrackingEnabled).isTrue()
                assertThat(prefs.lowBatteryThresholdPercent).isEqualTo(UserPreferences.DEFAULT_LOW_BATTERY_THRESHOLD)
            }
        }

    @Test
    fun `multiple preferences can be set independently`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.saveApiToken("my-token")
            repository.setOnboardingCompleted()
            repository.setLowBatteryThreshold(30)
            repository.setSecurityEnabled(true)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.apiToken).isEqualTo("my-token")
                assertThat(prefs.isOnboardingCompleted).isTrue()
                assertThat(prefs.lowBatteryThresholdPercent).isEqualTo(30)
                assertThat(prefs.isSecurityEnabled).isTrue()
                // Other defaults should remain unchanged
                assertThat(prefs.isBatteryTrackingEnabled).isTrue()
                assertThat(prefs.isLowBatteryNotificationEnabled).isFalse()
            }
        }

    @Test
    fun `userPreferencesFlow emits updates when preferences change`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            repository.userPreferencesFlow.test {
                // Initial state
                val initial = awaitItem()
                assertThat(initial.apiToken).isNull()

                // When first change
                repository.saveApiToken("token-1")
                val updated1 = awaitItem()
                assertThat(updated1.apiToken).isEqualTo("token-1")

                // When second change
                repository.setOnboardingCompleted()
                val updated2 = awaitItem()
                assertThat(updated2.apiToken).isEqualTo("token-1")
                assertThat(updated2.isOnboardingCompleted).isTrue()

                // When third change
                repository.saveApiToken("token-2")
                val updated3 = awaitItem()
                assertThat(updated3.apiToken).isEqualTo("token-2")
                assertThat(updated3.isOnboardingCompleted).isTrue()
            }
        }

    @Test
    fun `saveApiToken handles empty string`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.saveApiToken("")

            // Then
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().apiToken).isEqualTo("")
            }
        }

    @Test
    fun `saveApiToken handles very long tokens`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()
            val longToken = "x".repeat(5000)

            // When
            repository.saveApiToken(longToken)

            // Then
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().apiToken).isEqualTo(longToken)
            }
        }

    @Test
    fun `saveApiToken handles special characters`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.saveApiToken("token-with-special!@#\$%^&*()")

            // Then
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().apiToken).isEqualTo("token-with-special!@#\$%^&*()")
            }
        }

    @Test
    fun `setLowBatteryThreshold handles boundary values`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // Test minimum
            repository.setLowBatteryThreshold(0)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().lowBatteryThresholdPercent).isEqualTo(0)
            }

            // Test maximum
            repository.setLowBatteryThreshold(100)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().lowBatteryThresholdPercent).isEqualTo(100)
            }
        }

    @Test
    fun `setLowBatteryThreshold handles negative values`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When - even though negative doesn't make sense, repository should store it
            repository.setLowBatteryThreshold(-10)

            // Then
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().lowBatteryThresholdPercent).isEqualTo(-10)
            }
        }

    @Test
    fun `setLowBatteryThreshold handles values over 100`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When - even though over 100 doesn't make sense, repository should store it
            repository.setLowBatteryThreshold(150)

            // Then
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().lowBatteryThresholdPercent).isEqualTo(150)
            }
        }

    @Test
    fun `all boolean flags can be toggled back and forth`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // Test battery tracking
            repository.setBatteryTrackingEnabled(false)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isBatteryTrackingEnabled).isFalse()
            }
            repository.setBatteryTrackingEnabled(true)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isBatteryTrackingEnabled).isTrue()
            }

            // Test low battery notification
            repository.setLowBatteryNotificationEnabled(true)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isLowBatteryNotificationEnabled).isTrue()
            }
            repository.setLowBatteryNotificationEnabled(false)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isLowBatteryNotificationEnabled).isFalse()
            }

            // Test RSS feed content
            repository.setRssFeedContentEnabled(true)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isRssFeedContentEnabled).isTrue()
            }
            repository.setRssFeedContentEnabled(false)
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isRssFeedContentEnabled).isFalse()
            }
        }

    @Test
    fun `clearAll restores all preferences to defaults`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // Set all preferences to non-default values
            repository.saveApiToken("test-token")
            repository.setOnboardingCompleted()
            repository.setBatteryTrackingEnabled(false)
            repository.setLowBatteryNotificationEnabled(true)
            repository.setLowBatteryThreshold(25)
            repository.setRssFeedContentEnabled(true)
            repository.setRssFeedContentNotificationEnabled(true)
            repository.setAnnouncementAuthBannerDismissed(true)
            repository.setSecurityEnabled(true)

            // When
            repository.clearAll()

            // Then - verify ALL fields are back to defaults
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.apiToken).isNull()
                assertThat(prefs.isOnboardingCompleted).isFalse()
                assertThat(prefs.isBatteryTrackingEnabled).isTrue()
                assertThat(prefs.isLowBatteryNotificationEnabled).isFalse()
                assertThat(prefs.lowBatteryThresholdPercent).isEqualTo(UserPreferences.DEFAULT_LOW_BATTERY_THRESHOLD)
                assertThat(prefs.isRssFeedContentEnabled).isFalse()
                assertThat(prefs.isRssFeedContentNotificationEnabled).isFalse()
                assertThat(prefs.isAnnouncementAuthBannerDismissed).isFalse()
                assertThat(prefs.isSecurityEnabled).isFalse()
            }
        }

    @Test
    fun `onboarding completed cannot be reversed`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When
            repository.setOnboardingCompleted()

            // Then - there's no method to set it back to false
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isOnboardingCompleted).isTrue()
            }

            // Only clearAll can reset it
            repository.clearAll()
            repository.userPreferencesFlow.test {
                assertThat(awaitItem().isOnboardingCompleted).isFalse()
            }
        }

    @Test
    fun `multiple rapid changes emit all updates`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            repository.userPreferencesFlow.test {
                val initial = awaitItem()
                assertThat(initial.apiToken).isNull()

                // When - make multiple rapid changes
                repository.saveApiToken("token-1")
                assertThat(awaitItem().apiToken).isEqualTo("token-1")

                repository.saveApiToken("token-2")
                assertThat(awaitItem().apiToken).isEqualTo("token-2")

                repository.saveApiToken("token-3")
                assertThat(awaitItem().apiToken).isEqualTo("token-3")

                repository.clearApiToken()
                assertThat(awaitItem().apiToken).isNull()
            }
        }

    @Test
    fun `setRssFeedContentEnabled and setRssFeedContentNotificationEnabled are independent`() =
        runTest {
            // Given
            val repository = FakeUserPreferencesRepository()

            // When - enable content but not notifications
            repository.setRssFeedContentEnabled(true)
            repository.setRssFeedContentNotificationEnabled(false)

            // Then
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isRssFeedContentEnabled).isTrue()
                assertThat(prefs.isRssFeedContentNotificationEnabled).isFalse()
            }

            // When - enable notifications but disable content
            repository.setRssFeedContentEnabled(false)
            repository.setRssFeedContentNotificationEnabled(true)

            // Then - they remain independent
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.isRssFeedContentEnabled).isFalse()
                assertThat(prefs.isRssFeedContentNotificationEnabled).isTrue()
            }
        }

    /**
     * Fake in-memory implementation of UserPreferencesRepository for testing.
     */
    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val prefsFlow = MutableStateFlow(UserPreferences())

        override val userPreferencesFlow: Flow<UserPreferences> = prefsFlow

        override suspend fun saveApiToken(token: String) {
            prefsFlow.value = prefsFlow.value.copy(apiToken = token)
        }

        override suspend fun clearApiToken() {
            prefsFlow.value = prefsFlow.value.copy(apiToken = null)
        }

        override suspend fun setOnboardingCompleted() {
            prefsFlow.value = prefsFlow.value.copy(isOnboardingCompleted = true)
        }

        override suspend fun setBatteryTrackingEnabled(enabled: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(isBatteryTrackingEnabled = enabled)
        }

        override suspend fun setLowBatteryNotificationEnabled(enabled: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(isLowBatteryNotificationEnabled = enabled)
        }

        override suspend fun setLowBatteryThreshold(percent: Int) {
            prefsFlow.value = prefsFlow.value.copy(lowBatteryThresholdPercent = percent)
        }

        override suspend fun setRssFeedContentEnabled(enabled: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(isRssFeedContentEnabled = enabled)
        }

        override suspend fun setRssFeedContentNotificationEnabled(enabled: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(isRssFeedContentNotificationEnabled = enabled)
        }

        override suspend fun setAnnouncementAuthBannerDismissed(dismissed: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(isAnnouncementAuthBannerDismissed = dismissed)
        }

        override suspend fun setSecurityEnabled(enabled: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(isSecurityEnabled = enabled)
        }

        override suspend fun clearAll() {
            prefsFlow.value = UserPreferences()
        }
    }
}
