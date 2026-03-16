package ink.trmnl.android.buddy.ui.devicedetail
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import ink.trmnl.android.buddy.fakes.FakePlaylistItemsRepository
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [DeviceDetailPresenter].
 *
 * Tests cover:
 * - Initial state composition and device data display
 * - Device token detection
 * - Low battery notification settings
 * - Navigation events (back, settings)
 * - Playlist items prefetching and loading states
 * - Edge cases (null values)
 *
 * Battery history loading, analysis, and recording are tested separately in
 * [BatteryChartScreenTest] which covers [BatteryChartPresenter].
 */
class DeviceDetailScreenTest {
    @Test
    fun `presenter displays initial device state correctly`() =
        runTest {
            // Given
            val screen =
                DeviceDetailScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 85.0,
                    currentVoltage = 3.7,
                    wifiStrength = 75.0,
                    rssi = -60,
                    refreshRate = 300,
                )
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for initial state
                val state = awaitItem()

                // Verify device information is correctly displayed
                assertThat(state.deviceId).isEqualTo("ABC-123")
                assertThat(state.deviceName).isEqualTo("Test Device")
                assertThat(state.currentBattery).isEqualTo(85.0)
                assertThat(state.currentVoltage).isEqualTo(3.7)
                assertThat(state.wifiStrength).isEqualTo(75.0)
                assertThat(state.rssi).isEqualTo(-60)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter detects device token presence`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository(hasToken = true)
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize (playlist loading completes)
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify device token is detected
                assertThat(state.hasDeviceToken).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter detects missing device token`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository(hasToken = false)
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify no device token
                assertThat(state.hasDeviceToken).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter loads low battery notification settings`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isLowBatteryNotificationEnabled = true,
                            lowBatteryThresholdPercent = 25,
                        ),
                )
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify low battery settings
                assertThat(state.isLowBatteryNotificationEnabled).isTrue()
                assertThat(state.lowBatteryThresholdPercent).isEqualTo(25)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles back button navigation`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Send back event
                state.eventSink(DeviceDetailScreen.Event.BackClicked)

                // Verify navigation
                assertThat(navigator.awaitPop()).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles settings button navigation`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Send settings event
                state.eventSink(DeviceDetailScreen.Event.SettingsClicked)

                // Verify navigation to device token screen
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles null battery voltage`() =
        runTest {
            // Given
            val screen =
                DeviceDetailScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 85.0,
                    currentVoltage = null, // Null voltage
                    wifiStrength = 75.0,
                    rssi = -60,
                    refreshRate = 300,
                )
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify null voltage is handled
                assertThat(state.currentVoltage).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles null RSSI`() =
        runTest {
            // Given
            val screen =
                DeviceDetailScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 85.0,
                    currentVoltage = 3.7,
                    wifiStrength = 75.0,
                    rssi = null, // Null RSSI
                    refreshRate = null, // Null refresh rate
                )
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify null RSSI is handled
                assertThat(state.rssi).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter prefetches playlist items when deviceNumericId is provided`() =
        runTest {
            // Given
            val screen =
                DeviceDetailScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 85.0,
                    currentVoltage = 3.7,
                    wifiStrength = 75.0,
                    rssi = -60,
                    refreshRate = 300,
                    deviceNumericId = 12345,
                )
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for initial state with loading=true
                var state = awaitItem()
                assertThat(state.isPlaylistItemsLoading).isTrue()

                // Wait for loading to complete
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify prefetch completed
                assertThat(state.isPlaylistItemsLoading).isFalse()
                assertThat(playlistItemsRepo.getPlaylistItemsForDeviceCallCount).isEqualTo(1)
                assertThat(playlistItemsRepo.lastDeviceId).isEqualTo(12345)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles prefetch error gracefully`() =
        runTest {
            // Given
            val screen =
                DeviceDetailScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 85.0,
                    currentVoltage = 3.7,
                    wifiStrength = 75.0,
                    rssi = -60,
                    refreshRate = 300,
                    deviceNumericId = 12345,
                )
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo =
                FakePlaylistItemsRepository(
                    initialResult = Result.failure(Exception("Network error")),
                )
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for loading to complete
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify loading completes even on error
                assertThat(state.isPlaylistItemsLoading).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter does not prefetch when deviceNumericId is null`() =
        runTest {
            // Given
            val screen =
                DeviceDetailScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 85.0,
                    currentVoltage = 3.7,
                    wifiStrength = 75.0,
                    rssi = -60,
                    refreshRate = 300,
                    deviceNumericId = null,
                )
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // Wait for loading to complete
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Verify no prefetch attempt and loading state is cleared
                assertThat(state.isPlaylistItemsLoading).isFalse()
                assertThat(playlistItemsRepo.getPlaylistItemsForDeviceCallCount).isEqualTo(0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter transitions isPlaylistItemsLoading from true to false after successful prefetch`() =
        runTest {
            // Given
            val screen =
                DeviceDetailScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 85.0,
                    currentVoltage = 3.7,
                    wifiStrength = 75.0,
                    rssi = -60,
                    refreshRate = 300,
                    deviceNumericId = 12345,
                )
            val navigator = FakeNavigator(screen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val playlistItemsRepo = FakePlaylistItemsRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, userPrefsRepo, deviceTokenRepo, playlistItemsRepo)

            // When/Then
            presenter.test {
                // First state should have loading=true
                val initialState = awaitItem()
                assertThat(initialState.isPlaylistItemsLoading).isTrue()

                // Wait for loading to complete
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isPlaylistItemsLoading)

                // Final state should have loading=false
                assertThat(state.isPlaylistItemsLoading).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Helper function to create a test screen with default values.
     */
    private fun createTestScreen(): DeviceDetailScreen =
        DeviceDetailScreen(
            deviceId = "ABC-123",
            deviceName = "Test Device",
            currentBattery = 85.0,
            currentVoltage = 3.7,
            wifiStrength = 75.0,
            rssi = -60,
            refreshRate = 300,
        )

    /**
     * Helper function to create a test playlist item.
     */
    private fun createTestPlaylistItem(
        id: Int = 1,
        deviceId: Int = 1,
        displayName: String = "Test Plugin $id",
        isVisible: Boolean = true,
        isMashup: Boolean = false,
        isNeverRendered: Boolean = false,
        renderedAt: String? = null,
        rowOrder: Long = id.toLong(),
        pluginName: String? = "Test Plugin $id",
        mashupId: Int? = null,
    ): PlaylistItemUi =
        PlaylistItemUi(
            id = id,
            deviceId = deviceId,
            displayName = displayName,
            isVisible = isVisible,
            isMashup = isMashup,
            isNeverRendered = isNeverRendered,
            renderedAt = renderedAt,
            rowOrder = rowOrder,
            pluginName = pluginName,
            mashupId = mashupId,
        )
}
