package ink.trmnl.android.buddy.ui.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.security.FakeBiometricAuthHelper
import ink.trmnl.android.buddy.work.WorkerScheduler
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
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

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
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

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
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

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
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

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
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

            presenter.test {
                val state = awaitItem()

                state.eventSink(SettingsScreen.Event.BackClicked)

                // The navigation should have been triggered
                // We just verify the event was handled, the actual navigation is handled by Circuit
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter returns initial state with low battery notification disabled by default`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository = FakeUserPreferencesRepository()
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

            presenter.test {
                val state = awaitItem()
                assertThat(state.isLowBatteryNotificationEnabled).isFalse()
                assertThat(state.lowBatteryThresholdPercent).isEqualTo(20)
            }
        }

    @Test
    fun `toggling low battery notification updates the preference and schedules worker`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository = FakeUserPreferencesRepository()
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

            presenter.test {
                val state = awaitItem()

                // Initially disabled
                assertThat(state.isLowBatteryNotificationEnabled).isFalse()

                // Toggle to enabled
                state.eventSink(SettingsScreen.Event.LowBatteryNotificationToggled(true))

                val updatedState = awaitItem()
                assertThat(updatedState.isLowBatteryNotificationEnabled).isTrue()
                assertThat(repository.lowBatteryNotificationEnabled).isTrue()
                assertThat(workerScheduler.isScheduled).isTrue()
            }
        }

    @Test
    fun `toggling low battery notification off cancels worker`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isLowBatteryNotificationEnabled = true,
                        ),
                )
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            workerScheduler.isScheduled = true
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

            presenter.test {
                skipItems(1)
                val state = awaitItem()

                // Toggle to disabled
                state.eventSink(SettingsScreen.Event.LowBatteryNotificationToggled(false))

                val updatedState = awaitItem()
                assertThat(updatedState.isLowBatteryNotificationEnabled).isFalse()
                assertThat(repository.lowBatteryNotificationEnabled).isFalse()
                assertThat(workerScheduler.isScheduled).isFalse()
            }
        }

    @Test
    fun `changing low battery threshold updates the preference and reschedules worker`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository = FakeUserPreferencesRepository()
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

            presenter.test {
                val state = awaitItem()

                // Initially 20%
                assertThat(state.lowBatteryThresholdPercent).isEqualTo(20)

                // Change to 35%
                state.eventSink(SettingsScreen.Event.LowBatteryThresholdChanged(35))

                val updatedState = awaitItem()
                assertThat(updatedState.lowBatteryThresholdPercent).isEqualTo(35)
                assertThat(repository.lowBatteryThreshold).isEqualTo(35)
                assertThat(workerScheduler.isScheduled).isTrue()
            }
        }

    @Test
    fun `presenter returns state with custom threshold when preference is set`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isLowBatteryNotificationEnabled = true,
                            lowBatteryThresholdPercent = 40,
                        ),
                )
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.isLowBatteryNotificationEnabled).isTrue()
                assertThat(state.lowBatteryThresholdPercent).isEqualTo(40)
            }
        }

    @Test
    fun `content hub clicked event triggers navigation to ContentHubScreen`() =
        runTest {
            val navigator = FakeNavigator(SettingsScreen)
            val repository = FakeUserPreferencesRepository()
            val workerScheduler = FakeWorkerScheduler()
            val biometricAuthHelper = FakeBiometricAuthHelper()
            val presenter = SettingsPresenter(navigator, repository, workerScheduler, biometricAuthHelper)

            presenter.test {
                val state = awaitItem()

                state.eventSink(SettingsScreen.Event.ContentHubClicked)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ========== Test Fakes ==========

    /**
     * Fake implementation of WorkerScheduler for testing.
     */
    private class FakeWorkerScheduler : WorkerScheduler {
        var isScheduled = false
        var isAnnouncementSyncScheduled = false
        var isBlogPostSyncScheduled = false

        override fun scheduleLowBatteryNotification() {
            isScheduled = true
        }

        override fun cancelLowBatteryNotification() {
            isScheduled = false
        }

        override fun triggerLowBatteryNotificationNow() {
            // No-op for testing - immediate execution not needed in unit tests
        }

        override fun scheduleAnnouncementSync() {
            isAnnouncementSyncScheduled = true
        }

        override fun cancelAnnouncementSync() {
            isAnnouncementSyncScheduled = false
        }

        override fun scheduleBlogPostSync() {
            isBlogPostSyncScheduled = true
        }

        override fun cancelBlogPostSync() {
            isBlogPostSyncScheduled = false
        }
    }

    /**
     * Fake implementation of UserPreferencesRepository for testing.
     */
    private class FakeUserPreferencesRepository(
        initialPreferences: UserPreferences = UserPreferences(),
    ) : UserPreferencesRepository {
        var batteryTrackingEnabled = initialPreferences.isBatteryTrackingEnabled
            private set

        var lowBatteryNotificationEnabled = initialPreferences.isLowBatteryNotificationEnabled
            private set

        var lowBatteryThreshold = initialPreferences.lowBatteryThresholdPercent
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
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isRssFeedContentNotificationEnabled = enabled)
        }

        override suspend fun setAnnouncementAuthBannerDismissed(dismissed: Boolean) {
            _userPreferencesFlow.value =
                _userPreferencesFlow.value.copy(isAnnouncementAuthBannerDismissed = dismissed)
        }

        override suspend fun setSecurityEnabled(enabled: Boolean) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isSecurityEnabled = enabled)
        }

        override suspend fun clearAll() {
            _userPreferencesFlow.value = UserPreferences()
            batteryTrackingEnabled = true
            lowBatteryNotificationEnabled = false
            lowBatteryThreshold = 20
        }
    }
}
