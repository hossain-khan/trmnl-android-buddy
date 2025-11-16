package ink.trmnl.android.buddy.ui.accesstoken

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for AccessTokenPresenter.
 *
 * Tests cover:
 * - Initial empty state
 * - Token input and validation
 * - Save token to preferences
 * - Navigation flows
 * - Error handling
 * - Edge cases (long tokens, special characters, unicode, whitespace)
 */
class AccessTokenScreenTest {
    @Test
    fun `presenter returns initial empty state`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                assertThat(state.token).isEqualTo("")
                assertThat(state.isLoading).isFalse()
                assertThat(state.errorMessage).isNull()
            }
        }

    @Test
    fun `token changed event updates state and clears error`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Initially empty
                assertThat(state.token).isEqualTo("")

                // Change token
                state.eventSink(AccessTokenScreen.Event.TokenChanged("test_token_123"))

                val updatedState = awaitItem()
                assertThat(updatedState.token).isEqualTo("test_token_123")
                assertThat(updatedState.errorMessage).isNull()
            }
        }

    @Test
    fun `save token with empty token shows error`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Try to save with empty token
                state.eventSink(AccessTokenScreen.Event.SaveClicked)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token cannot be empty")
                assertThat(errorState.isLoading).isFalse()
            }
        }

    @Test
    fun `save token with blank token shows error`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Set blank token (only whitespace)
                state.eventSink(AccessTokenScreen.Event.TokenChanged("   "))
                awaitItem()

                // Try to save
                state.eventSink(AccessTokenScreen.Event.SaveClicked)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token cannot be empty")
            }
        }

    @Test
    fun `save token with short token shows error`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Set short token (less than 10 characters)
                state.eventSink(AccessTokenScreen.Event.TokenChanged("short"))
                awaitItem()

                // Try to save
                state.eventSink(AccessTokenScreen.Event.SaveClicked)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token appears to be too short")
            }
        }

    @Test
    fun `save valid token succeeds and updates repository`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                var state = awaitItem()

                // Set valid token (10+ characters)
                state.eventSink(AccessTokenScreen.Event.TokenChanged("valid_token_123"))
                state = awaitItem()

                // Save token
                state.eventSink(AccessTokenScreen.Event.SaveClicked)

                // Should show loading state
                state = awaitItem()
                assertThat(state.isLoading).isTrue()
                assertThat(state.errorMessage).isNull()

                // Wait for completion (loading becomes false)
                state = awaitItem()
                assertThat(state.isLoading).isFalse()

                // Verify token was saved (trimmed)
                assertThat(repository.savedToken).isEqualTo("valid_token_123")

                // Verify onboarding was completed
                assertThat(repository.onboardingCompleted).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `save token trims whitespace before saving`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Set token with leading and trailing whitespace
                state.eventSink(AccessTokenScreen.Event.TokenChanged("  token_with_spaces  "))
                awaitItem()

                // Save token
                state.eventSink(AccessTokenScreen.Event.SaveClicked)
                awaitItem() // loading
                awaitItem() // final

                // Verify token was trimmed
                assertThat(repository.savedToken).isEqualTo("token_with_spaces")
            }
        }

    @Test
    fun `save token handles very long tokens`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Create a very long token (1000+ characters)
                val longToken = "user_" + "x".repeat(1000)
                state.eventSink(AccessTokenScreen.Event.TokenChanged(longToken))
                awaitItem()

                // Save token
                state.eventSink(AccessTokenScreen.Event.SaveClicked)
                awaitItem() // loading
                awaitItem() // final

                // Verify long token was saved
                assertThat(repository.savedToken).isEqualTo(longToken)
            }
        }

    @Test
    fun `save token handles special characters`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                val specialToken = "token_!@#$%^&*()_+-=[]{}|;':,.<>?/"
                state.eventSink(AccessTokenScreen.Event.TokenChanged(specialToken))
                awaitItem()

                state.eventSink(AccessTokenScreen.Event.SaveClicked)
                awaitItem() // loading
                awaitItem() // final

                assertThat(repository.savedToken).isEqualTo(specialToken)
            }
        }

    @Test
    fun `save token handles unicode characters`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                val unicodeToken = "token_🚀日本語العربية"
                state.eventSink(AccessTokenScreen.Event.TokenChanged(unicodeToken))
                awaitItem()

                state.eventSink(AccessTokenScreen.Event.SaveClicked)
                awaitItem() // loading
                awaitItem() // final

                assertThat(repository.savedToken).isEqualTo(unicodeToken)
            }
        }

    @Test
    fun `save token handles exception and shows error`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository =
                FakeUserPreferencesRepository(
                    shouldThrowOnSave = true,
                )
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(AccessTokenScreen.Event.TokenChanged("valid_token_123"))
                awaitItem()

                state.eventSink(AccessTokenScreen.Event.SaveClicked)
                awaitItem() // loading

                // Should show error and stop loading
                val errorState = awaitItem()
                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("Failed to save token: Test exception")
            }
        }

    @Test
    fun `multiple token changes update state correctly`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // First change
                state.eventSink(AccessTokenScreen.Event.TokenChanged("token1"))
                val state1 = awaitItem()
                assertThat(state1.token).isEqualTo("token1")

                // Second change
                state1.eventSink(AccessTokenScreen.Event.TokenChanged("token2"))
                val state2 = awaitItem()
                assertThat(state2.token).isEqualTo("token2")

                // Third change
                state2.eventSink(AccessTokenScreen.Event.TokenChanged("token3"))
                val state3 = awaitItem()
                assertThat(state3.token).isEqualTo("token3")
            }
        }

    @Test
    fun `save token exactly 10 characters succeeds`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Exactly 10 characters (boundary case)
                state.eventSink(AccessTokenScreen.Event.TokenChanged("1234567890"))
                awaitItem()

                state.eventSink(AccessTokenScreen.Event.SaveClicked)
                awaitItem() // loading
                awaitItem() // final

                // Should succeed
                assertThat(repository.savedToken).isEqualTo("1234567890")
            }
        }

    @Test
    fun `save token with 9 characters shows error`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // 9 characters (boundary case - should fail)
                state.eventSink(AccessTokenScreen.Event.TokenChanged("123456789"))
                awaitItem()

                state.eventSink(AccessTokenScreen.Event.SaveClicked)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token appears to be too short")
            }
        }

    @Test
    fun `save token with tabs and newlines is trimmed`() =
        runTest {
            val navigator = FakeNavigator(AccessTokenScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = AccessTokenPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(AccessTokenScreen.Event.TokenChanged("\t\ntoken_value\n\t"))
                awaitItem()

                state.eventSink(AccessTokenScreen.Event.SaveClicked)
                awaitItem() // loading
                awaitItem() // final

                assertThat(repository.savedToken).isEqualTo("token_value")
            }
        }

    /**
     * Fake implementation of UserPreferencesRepository for testing.
     */
    private class FakeUserPreferencesRepository(
        private val shouldThrowOnSave: Boolean = false,
    ) : UserPreferencesRepository {
        var savedToken: String? = null
        var onboardingCompleted: Boolean = false

        private val prefsFlow = MutableStateFlow(UserPreferences())

        override val userPreferencesFlow: Flow<UserPreferences> = prefsFlow

        override suspend fun saveApiToken(token: String) {
            if (shouldThrowOnSave) {
                throw Exception("Test exception")
            }
            savedToken = token
            prefsFlow.value = prefsFlow.value.copy(apiToken = token)
        }

        override suspend fun clearApiToken() {
            savedToken = null
            prefsFlow.value = prefsFlow.value.copy(apiToken = null)
        }

        override suspend fun setOnboardingCompleted() {
            onboardingCompleted = true
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
            savedToken = null
            onboardingCompleted = false
            prefsFlow.value = UserPreferences()
        }
    }
}
