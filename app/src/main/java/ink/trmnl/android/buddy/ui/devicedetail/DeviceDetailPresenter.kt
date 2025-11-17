package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.devicetoken.DeviceTokenScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Presenter for DeviceDetailScreen.
 */
@Inject
class DeviceDetailPresenter
    constructor(
        @Assisted private val screen: DeviceDetailScreen,
        @Assisted private val navigator: Navigator,
        private val batteryHistoryRepository: BatteryHistoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val deviceTokenRepository: DeviceTokenRepository,
    ) : Presenter<DeviceDetailScreen.State> {
        @Composable
        override fun present(): DeviceDetailScreen.State {
            val batteryHistory by batteryHistoryRepository
                .getBatteryHistoryForDevice(screen.deviceId)
                .collectAsState(initial = emptyList())
            var isLoading by rememberRetained { mutableStateOf(true) }
            var hasDeviceToken by rememberRetained { mutableStateOf(false) }
            var isLowBatteryNotificationEnabled by rememberRetained { mutableStateOf(false) }
            var lowBatteryThresholdPercent by rememberRetained { mutableStateOf(20) }
            val preferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences(),
            )
            val coroutineScope = rememberCoroutineScope()

            // Update low battery preferences from user preferences
            LaunchedEffect(preferences) {
                isLowBatteryNotificationEnabled = preferences.isLowBatteryNotificationEnabled
                lowBatteryThresholdPercent = preferences.lowBatteryThresholdPercent
            }

            // Check if device token exists
            LaunchedEffect(screen.deviceId) {
                val token = deviceTokenRepository.getDeviceToken(screen.deviceId)
                hasDeviceToken = token != null
            }

            // Check if battery has been recorded today
            val hasRecordedToday by remember {
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
            val clearHistoryReason by remember {
                derivedStateOf {
                    BatteryHistoryAnalyzer.getClearHistoryReason(batteryHistory)
                }
            }

            // Mark loading complete when we have data or after initial load
            LaunchedEffect(batteryHistory) {
                isLoading = false
            }

            return DeviceDetailScreen.State(
                deviceId = screen.deviceId,
                deviceName = screen.deviceName,
                currentBattery = screen.currentBattery,
                currentVoltage = screen.currentVoltage,
                wifiStrength = screen.wifiStrength,
                rssi = screen.rssi,
                batteryHistory = batteryHistory,
                isLoading = isLoading,
                isBatteryTrackingEnabled = preferences.isBatteryTrackingEnabled,
                hasRecordedToday = hasRecordedToday,
                hasDeviceToken = hasDeviceToken,
                clearHistoryReason = clearHistoryReason,
                isLowBatteryNotificationEnabled = isLowBatteryNotificationEnabled,
                lowBatteryThresholdPercent = lowBatteryThresholdPercent,
            ) { event ->
                when (event) {
                    DeviceDetailScreen.Event.BackClicked -> navigator.pop()
                    DeviceDetailScreen.Event.SettingsClicked -> {
                        navigator.goTo(
                            DeviceTokenScreen(
                                deviceFriendlyId = screen.deviceId,
                                deviceName = screen.deviceName,
                            ),
                        )
                    }
                    is DeviceDetailScreen.Event.PopulateBatteryHistory -> {
                        // Generate simulated battery history data
                        coroutineScope.launch(Dispatchers.IO) {
                            val currentTime = System.currentTimeMillis()
                            val weeksToGenerate = 12 // Generate 12 weeks of history
                            val currentBattery = screen.currentBattery
                            val minBattery = event.minBatteryLevel.toDouble()

                            // Calculate battery drop per week
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
                    DeviceDetailScreen.Event.ClearBatteryHistory -> {
                        // Clear all battery history for this device
                        coroutineScope.launch(Dispatchers.IO) {
                            batteryHistoryRepository.deleteHistoryForDevice(screen.deviceId)
                        }
                    }
                    DeviceDetailScreen.Event.RecordBatteryManually -> {
                        // Record current battery level manually
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

        @CircuitInject(DeviceDetailScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: DeviceDetailScreen,
                navigator: Navigator,
            ): DeviceDetailPresenter
        }
    }
