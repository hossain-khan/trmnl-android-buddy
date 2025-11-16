package ink.trmnl.android.buddy.ui.devicedetail
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.battery.BatteryHistoryAnalyzer
import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import ink.trmnl.android.buddy.data.database.BatteryHistoryRepository
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for DeviceDetailScreen presenter.
 *
 * Tests cover:
 * - Initial state composition and device data display
 * - Battery history loading and display
 * - Battery tracking state management
 * - User interactions (back, settings navigation)
 * - Manual battery recording
 * - Clear battery history logic
 * - Low battery notification settings
 * - Edge cases (null values, empty history, no device token)
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
                )
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for initial state
                var state = awaitItem()

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
    fun `presenter loads battery history on composition`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryHistory = createTestBatteryHistory(screen.deviceId, count = 5)
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait until battery history is loaded
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify battery history is loaded
                assertThat(state.isLoading).isFalse()
                assertThat(state.batteryHistory).hasSize(5)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows empty battery history when none exists`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for loading to complete
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify empty battery history
                assertThat(state.batteryHistory).hasSize(0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter respects battery tracking enabled preference`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isBatteryTrackingEnabled = true),
                )
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify battery tracking is enabled
                assertThat(state.isBatteryTrackingEnabled).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter respects battery tracking disabled preference`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isBatteryTrackingEnabled = false),
                )
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify battery tracking is disabled
                assertThat(state.isBatteryTrackingEnabled).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter detects device token presence`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository(hasToken = true)
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

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
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository(hasToken = false)
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify no device token
                assertThat(state.hasDeviceToken).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter calculates hasRecordedToday correctly when recording exists`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val todayTimestamp = System.currentTimeMillis()
            val batteryHistory =
                listOf(
                    BatteryHistoryEntity(
                        deviceId = screen.deviceId,
                        percentCharged = 80.0,
                        batteryVoltage = 3.7,
                        timestamp = todayTimestamp,
                    ),
                )
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify hasRecordedToday is true
                assertThat(state.hasRecordedToday).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter calculates hasRecordedToday correctly when no recording today`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val yesterdayTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            val batteryHistory =
                listOf(
                    BatteryHistoryEntity(
                        deviceId = screen.deviceId,
                        percentCharged = 80.0,
                        batteryVoltage = 3.7,
                        timestamp = yesterdayTimestamp,
                    ),
                )
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify hasRecordedToday is false
                assertThat(state.hasRecordedToday).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter detects charging event in battery history`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val now = System.currentTimeMillis()
            // Create history with charging event (battery jump > 50%)
            val batteryHistory =
                listOf(
                    BatteryHistoryEntity(
                        deviceId = screen.deviceId,
                        percentCharged = 30.0,
                        batteryVoltage = 3.5,
                        timestamp = now - TimeUnit.DAYS.toMillis(2),
                    ),
                    BatteryHistoryEntity(
                        deviceId = screen.deviceId,
                        percentCharged = 90.0, // Jump of 60% indicates charging
                        batteryVoltage = 4.1,
                        timestamp = now - TimeUnit.DAYS.toMillis(1),
                    ),
                )
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify clear history reason is set for charging
                assertThat(state.clearHistoryReason).isNotNull()
                assertThat(state.clearHistoryReason).isEqualTo(BatteryHistoryAnalyzer.ClearHistoryReason.CHARGING_DETECTED)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter detects stale battery history data`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val now = System.currentTimeMillis()
            // Create history with data older than 6 months
            val batteryHistory =
                listOf(
                    BatteryHistoryEntity(
                        deviceId = screen.deviceId,
                        percentCharged = 80.0,
                        batteryVoltage = 3.7,
                        timestamp = now - TimeUnit.DAYS.toMillis(200), // > 6 months
                    ),
                )
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify clear history reason is set for stale data
                assertThat(state.clearHistoryReason).isNotNull()
                assertThat(state.clearHistoryReason).isEqualTo(BatteryHistoryAnalyzer.ClearHistoryReason.STALE_DATA)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows no clear history reason for clean data`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryHistory = createTestBatteryHistory(screen.deviceId, count = 3)
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify no clear history reason
                assertThat(state.clearHistoryReason).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter loads low battery notification settings`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isLowBatteryNotificationEnabled = true,
                            lowBatteryThresholdPercent = 25,
                        ),
                )
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

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
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

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
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Send settings event
                state.eventSink(DeviceDetailScreen.Event.SettingsClicked)

                // Verify navigation to device token screen
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter records battery manually`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Initially no recordings
                assertThat(state.batteryHistory).hasSize(0)

                // Send record battery event
                state.eventSink(DeviceDetailScreen.Event.RecordBatteryManually)

                // Advance time to allow the IO coroutine to complete
                testScheduler.advanceUntilIdle()

                // Wait for state update with new battery history
                var updatedState: DeviceDetailScreen.State
                do {
                    updatedState = awaitItem()
                } while (updatedState.batteryHistory.isEmpty())

                // Verify battery was recorded
                assertThat(updatedState.batteryHistory).hasSize(1)
                val recording = updatedState.batteryHistory.first()
                assertThat(recording.deviceId).isEqualTo(screen.deviceId)
                assertThat(recording.percentCharged).isEqualTo(screen.currentBattery)
                assertThat(recording.batteryVoltage).isEqualTo(screen.currentVoltage)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter clears battery history`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryHistory = createTestBatteryHistory(screen.deviceId, count = 5)
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify initial history
                assertThat(state.batteryHistory).hasSize(5)

                // Send clear history event
                state.eventSink(DeviceDetailScreen.Event.ClearBatteryHistory)

                // Advance time to allow the IO coroutine to complete
                testScheduler.advanceUntilIdle()

                // Wait for updated state after clearing
                do {
                    state = awaitItem()
                } while (state.batteryHistory.isNotEmpty())

                // Verify history was cleared
                assertThat(state.batteryHistory).hasSize(0)

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
                )
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

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
                )
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            val presenter = DeviceDetailPresenter(screen, navigator, batteryRepo, userPrefsRepo, deviceTokenRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: DeviceDetailScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify null RSSI is handled
                assertThat(state.rssi).isNull()

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
        )

    /**
     * Helper function to create test battery history.
     */
    private fun createTestBatteryHistory(
        deviceId: String,
        count: Int,
    ): List<BatteryHistoryEntity> {
        val now = System.currentTimeMillis()
        return (0 until count).map { index ->
            BatteryHistoryEntity(
                deviceId = deviceId,
                percentCharged = 90.0 - (index * 5.0), // Declining battery
                batteryVoltage = 3.8 - (index * 0.1),
                timestamp = now - TimeUnit.DAYS.toMillis(index * 7L), // Weekly readings
            )
        }
    }
}

/**
 * Fake implementation of BatteryHistoryRepository for testing.
 */
private class FakeBatteryHistoryRepository(
    initialHistory: List<BatteryHistoryEntity> = emptyList(),
) : BatteryHistoryRepository {
    private val historyFlow = MutableStateFlow(initialHistory)
    val recordedReadings = mutableListOf<BatteryHistoryEntity>()

    override suspend fun recordBatteryReading(
        deviceId: String,
        percentCharged: Double,
        batteryVoltage: Double?,
        timestamp: Long,
    ) {
        val entity =
            BatteryHistoryEntity(
                deviceId = deviceId,
                percentCharged = percentCharged,
                batteryVoltage = batteryVoltage,
                timestamp = timestamp,
            )
        recordedReadings.add(entity)
        historyFlow.value = historyFlow.value + entity
    }

    override fun getBatteryHistoryForDevice(deviceId: String): Flow<List<BatteryHistoryEntity>> = historyFlow

    override suspend fun getLatestBatteryReading(deviceId: String): BatteryHistoryEntity? = historyFlow.value.maxByOrNull { it.timestamp }

    override fun getBatteryHistoryInRange(
        deviceId: String,
        startTime: Long,
        endTime: Long,
    ): Flow<List<BatteryHistoryEntity>> = historyFlow

    override suspend fun deleteHistoryForDevice(deviceId: String) {
        historyFlow.value = emptyList()
    }
}
