package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.data.battery.BatteryHistoryAnalyzer
import ink.trmnl.android.buddy.data.database.BatteryHistoryRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Presenter for [BatteryChartScreen] — manages battery history data and user interactions.
 *
 * **Responsibilities:**
 * - Loads historical battery readings from the local Room database
 * - Analyzes battery health trends using [BatteryHistoryAnalyzer]
 * - Manages manual battery recording and history clearing
 * - Provides simulated test data for development via [BatteryChartScreen.Event.PopulateBatteryHistory]
 *
 * This presenter was extracted from `DeviceDetailPresenter` to improve separation of
 * concerns and make battery-specific logic independently testable.
 *
 * @property screen Screen parameters with device identification and current status
 * @property navigator Circuit navigator (unused for this sub-screen, provided for Circuit contract)
 * @property batteryHistoryRepository Repository for local battery history database operations
 * @property userPreferencesRepository Repository for user settings (battery tracking preference)
 *
 * @see BatteryChartScreen Screen definition with State and Event sealed classes
 * @see BatteryHistoryAnalyzer Battery health analysis and prediction logic
 */
@Inject
class BatteryChartPresenter
    constructor(
        @Assisted private val screen: BatteryChartScreen,
        @Assisted private val navigator: Navigator,
        private val batteryHistoryRepository: BatteryHistoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<BatteryChartScreen.State> {
        @Composable
        override fun present(): BatteryChartScreen.State {
            val batteryHistory by batteryHistoryRepository
                .getBatteryHistoryForDevice(screen.deviceId)
                .collectAsState(initial = emptyList())
            var isLoading by rememberRetained { mutableStateOf(true) }
            val preferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences(),
            )
            val coroutineScope = rememberCoroutineScope()

            // Check if battery has been recorded today
            val hasRecordedToday by androidx.compose.runtime.remember {
                derivedStateOf {
                    val today =
                        java.util.Calendar
                            .getInstance()
                            .apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis

                    batteryHistory.any { it.timestamp >= today }
                }
            }

            // Check if battery history should be cleared
            val clearHistoryReason by androidx.compose.runtime.remember {
                derivedStateOf {
                    BatteryHistoryAnalyzer.getClearHistoryReason(batteryHistory)
                }
            }

            // Mark loading complete when data arrives
            LaunchedEffect(batteryHistory) {
                isLoading = false
            }

            return BatteryChartScreen.State(
                batteryHistory = batteryHistory,
                isLoading = isLoading,
                isBatteryTrackingEnabled = preferences.isBatteryTrackingEnabled,
                hasRecordedToday = hasRecordedToday,
                clearHistoryReason = clearHistoryReason,
                currentBattery = screen.currentBattery,
            ) { event ->
                when (event) {
                    is BatteryChartScreen.Event.PopulateBatteryHistory -> {
                        coroutineScope.launch(Dispatchers.IO) {
                            val currentTime = System.currentTimeMillis()
                            val weeksToGenerate = 12
                            val currentBattery = screen.currentBattery
                            val minBattery = event.minBatteryLevel.toDouble()
                            val totalDrop = currentBattery - minBattery
                            val dropPerWeek = totalDrop / weeksToGenerate

                            for (week in 0 until weeksToGenerate) {
                                val weeklyBattery = currentBattery - (dropPerWeek * week)
                                val timestamp = currentTime - TimeUnit.DAYS.toMillis((weeksToGenerate - week) * 7L)

                                batteryHistoryRepository.recordBatteryReading(
                                    deviceId = screen.deviceId,
                                    percentCharged = weeklyBattery,
                                    batteryVoltage = screen.currentVoltage,
                                    timestamp = timestamp,
                                )
                            }
                        }
                    }
                    BatteryChartScreen.Event.ClearBatteryHistory -> {
                        coroutineScope.launch(Dispatchers.IO) {
                            batteryHistoryRepository.deleteHistoryForDevice(screen.deviceId)
                        }
                    }
                    BatteryChartScreen.Event.RecordBatteryManually -> {
                        coroutineScope.launch(Dispatchers.IO) {
                            batteryHistoryRepository.recordBatteryReading(
                                deviceId = screen.deviceId,
                                percentCharged = screen.currentBattery,
                                batteryVoltage = screen.currentVoltage,
                            )
                        }
                    }
                }
            }
        }

        @CircuitInject(BatteryChartScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: BatteryChartScreen,
                navigator: Navigator,
            ): BatteryChartPresenter
        }
    }
