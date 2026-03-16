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
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeBatteryHistoryRepository
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [BatteryChartPresenter].
 *
 * Tests cover:
 * - Battery history loading and display
 * - Battery tracking preference state
 * - hasRecordedToday calculation
 * - clearHistoryReason detection (charging, stale data)
 * - Manual battery recording event
 * - Clear battery history event
 * - Low battery preference forwarding
 * - Edge cases (empty history, null voltage)
 */
class BatteryChartScreenTest {
    // ====================== Initial state ======================

    @Test
    fun `presenter loads battery history on composition`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryHistory = createTestBatteryHistory(screen.deviceId, count = 5)
            val batteryRepo = FakeBatteryHistoryRepository(initialHistory = batteryHistory)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                // Wait until battery history is loaded
                var state: BatteryChartScreen.State
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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                // Wait for loading to complete
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify empty battery history
                assertThat(state.batteryHistory).hasSize(0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ====================== Battery tracking preferences ======================

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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: BatteryChartScreen.State
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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify battery tracking is disabled
                assertThat(state.isBatteryTrackingEnabled).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ====================== hasRecordedToday ======================

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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.hasRecordedToday).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ====================== clearHistoryReason detection ======================

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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.clearHistoryReason).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ====================== Battery events ======================

    @Test
    fun `presenter records battery manually`() =
        runTest {
            // Given
            val screen = createTestScreen()
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Initially no recordings
                assertThat(state.batteryHistory).hasSize(0)

                // Send record battery event
                state.eventSink(BatteryChartScreen.Event.RecordBatteryManually)

                // Advance time to allow the IO coroutine to complete
                testScheduler.advanceUntilIdle()

                // Wait for state update with new battery history
                var updatedState: BatteryChartScreen.State
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
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                // Wait for state to stabilize
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Verify initial history
                assertThat(state.batteryHistory).hasSize(5)

                // Send clear history event
                state.eventSink(BatteryChartScreen.Event.ClearBatteryHistory)

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
    fun `presenter exposes currentBattery from screen in state`() =
        runTest {
            // Given
            val screen =
                BatteryChartScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    currentBattery = 75.0,
                    currentVoltage = 3.8,
                )
            val navigator = FakeNavigator(screen)
            val batteryRepo = FakeBatteryHistoryRepository()
            val userPrefsRepo = FakeUserPreferencesRepository()
            val presenter = BatteryChartPresenter(screen, navigator, batteryRepo, userPrefsRepo)

            // When/Then
            presenter.test {
                var state: BatteryChartScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.currentBattery).isEqualTo(75.0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ====================== Helpers ======================

    private fun createTestScreen(): BatteryChartScreen =
        BatteryChartScreen(
            deviceId = "ABC-123",
            deviceName = "Test Device",
            currentBattery = 85.0,
            currentVoltage = 3.7,
        )

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
