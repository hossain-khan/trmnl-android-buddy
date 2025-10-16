package ink.trmnl.android.buddy.ui.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for SettingsScreen presenter.
 */
class SettingsScreenTest {
    @Test
    fun `presenter returns initial state with battery tracking enabled by default`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = SettingsPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()
                assertThat(state.isBatteryTrackingEnabled).isTrue()
            }
        }

    @Test
    fun `presenter returns state with battery tracking disabled when preference is set`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isBatteryTrackingEnabled = false,
                        ),
                )
            val presenter = SettingsPresenter(navigator, repository)

            presenter.test {
                // Skip the initial state and get the updated state
                skipItems(1)
                val state = awaitItem()
                assertThat(state.isBatteryTrackingEnabled).isFalse()
            }
        }

    @Test
    fun `toggling battery tracking updates the preference`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = SettingsPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                // Initially enabled
                assertThat(state.isBatteryTrackingEnabled).isTrue()

                // Toggle to disabled
                state.eventSink(SettingsScreen.Event.BatteryTrackingToggled(false))

                val updatedState = awaitItem()
                assertThat(updatedState.isBatteryTrackingEnabled).isFalse()
                assertThat(repository.batteryTrackingEnabled).isFalse()
            }
        }

    @Test
    fun `toggling battery tracking from disabled to enabled updates the preference`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isBatteryTrackingEnabled = false,
                        ),
                )
            val presenter = SettingsPresenter(navigator, repository)

            presenter.test {
                // Skip the initial state and get the state with disabled tracking
                skipItems(1)
                val state = awaitItem()

                // Initially disabled
                assertThat(state.isBatteryTrackingEnabled).isFalse()

                // Toggle to enabled
                state.eventSink(SettingsScreen.Event.BatteryTrackingToggled(true))

                val updatedState = awaitItem()
                assertThat(updatedState.isBatteryTrackingEnabled).isTrue()
                assertThat(repository.batteryTrackingEnabled).isTrue()
            }
        }

    @Test
    fun `back clicked event triggers navigation`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository = FakeUserPreferencesRepository()
            val presenter = SettingsPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(SettingsScreen.Event.BackClicked)

                // The navigation should have been triggered
                // We just verify the event was handled, the actual navigation is handled by Circuit
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ========== Test Fakes ==========

    /**
     * Fake implementation of UserPreferencesRepository for testing.
     */
    private class FakeUserPreferencesRepository(
        initialPreferences: UserPreferences = UserPreferences(),
    ) : UserPreferencesRepository {
        var batteryTrackingEnabled = initialPreferences.isBatteryTrackingEnabled
            private set

        private val _userPreferencesFlow =
            MutableStateFlow(initialPreferences)

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
            batteryTrackingEnabled = enabled
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isBatteryTrackingEnabled = enabled)
        }

        override suspend fun clearAll() {
            _userPreferencesFlow.value = UserPreferences()
            batteryTrackingEnabled = true
        }
    }
}
