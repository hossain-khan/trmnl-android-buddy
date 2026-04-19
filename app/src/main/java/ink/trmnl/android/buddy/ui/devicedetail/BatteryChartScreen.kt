package ink.trmnl.android.buddy.ui.devicedetail

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.data.battery.BatteryHistoryAnalyzer
import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import kotlinx.parcelize.Parcelize

/**
 * Sub-screen for the battery history chart section of the device detail view.
 *
 * This screen encapsulates all battery-related state and logic that was previously
 * part of [DeviceDetailScreen], including:
 * - Loading and displaying battery history from local database
 * - Battery health trajectory analysis (via [BatteryHistoryAnalyzer])
 * - Manual battery data recording
 * - Battery history lifecycle (clear, populate test data)
 *
 * **Usage:**
 * This screen is embedded inside [DeviceDetailContent] using `CircuitContent`,
 * allowing it to be independently tested and maintained while composing naturally
 * within the parent screen layout.
 *
 * @property deviceId Unique friendly device ID for loading history
 * @property deviceName Device display name
 * @property currentBattery Current battery percentage (0.0 to 100.0)
 * @property currentVoltage Current battery voltage (may be null)
 *
 * @see BatteryChartPresenter Handles state management and business logic
 * @see BatteryChartContent Composable UI for this screen
 */
@Parcelize
data class BatteryChartScreen(
    val deviceId: String,
    val deviceName: String,
    val currentBattery: Double,
    val currentVoltage: Double?,
) : Screen {
    /**
     * UI state for the battery chart section.
     *
     * @property batteryHistory Historical battery readings from local database
     * @property isLoading True while initial data is loading
     * @property isBatteryTrackingEnabled Whether user has enabled battery tracking
     * @property hasRecordedToday Whether battery was manually recorded today
     * @property clearHistoryReason Reason to suggest clearing history (null if clean)
     * @property eventSink Handler for user interaction events
     */
    data class State(
        val batteryHistory: List<BatteryHistoryEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isBatteryTrackingEnabled: Boolean = true,
        val hasRecordedToday: Boolean = false,
        val clearHistoryReason: BatteryHistoryAnalyzer.ClearHistoryReason? = null,
        /**
         * Current battery percentage, passed through for display and debug panel use.
         */
        val currentBattery: Double = 0.0,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Events for the battery chart section.
     */
    sealed interface Event : CircuitUiEvent {
        /**
         * Developer action to populate battery history with simulated test data.
         *
         * @property minBatteryLevel Minimum battery level for generated data (0.0-100.0)
         */
        data class PopulateBatteryHistory(
            val minBatteryLevel: Float,
        ) : Event

        /**
         * User confirmed clearing all battery history for this device.
         */
        data object ClearBatteryHistory : Event

        /**
         * User tapped to manually record the current battery reading.
         */
        data object RecordBatteryManually : Event
    }
}
