package ink.trmnl.android.buddy.ui.devicedetail

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.data.battery.BatteryHistoryAnalyzer
import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying device details including battery history and health trajectory.
 */
@Parcelize
data class DeviceDetailScreen(
    val deviceId: String,
    val deviceName: String,
    val currentBattery: Double,
    val currentVoltage: Double?,
    val wifiStrength: Double,
    val rssi: Int?,
    val refreshRate: Int?, // in seconds, null if not available
    val deviceNumericId: Int? = null, // Numeric device ID for API calls, null if not available
) : Screen {
    data class State(
        val deviceId: String,
        val deviceName: String,
        val currentBattery: Double,
        val currentVoltage: Double?,
        val wifiStrength: Double,
        val rssi: Int?,
        val refreshRate: Int?, // in seconds, null if not available
        val batteryHistory: List<BatteryHistoryEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isBatteryTrackingEnabled: Boolean = true,
        val hasRecordedToday: Boolean = false,
        val hasDeviceToken: Boolean = false,
        val clearHistoryReason: BatteryHistoryAnalyzer.ClearHistoryReason? = null,
        val isLowBatteryNotificationEnabled: Boolean = false,
        val lowBatteryThresholdPercent: Int = 20,
        val isPlaylistItemsLoading: Boolean = true, // Tracks playlist items prefetch progress
        /**
         * Total number of playlist items for this device.
         * Calculated from the device-specific filtered items in the repository cache.
         * Updated reactively when the repository cache changes.
         */
        val playlistItemsCount: Int = 0,
        /**
         * Name of the currently playing/displayed item.
         * Determined by finding the item with the most recent renderedAt timestamp.
         * Falls back to the first item if none have been rendered yet.
         * Updated reactively when the repository cache changes.
         */
        val nowPlayingItem: String = "",
        val upNextItem: String = "", // Name of next queue item to play
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object SettingsClicked : Event()

        data object ViewPlaylistItems : Event()

        data class PopulateBatteryHistory(
            val minBatteryLevel: Float,
        ) : Event()

        data object ClearBatteryHistory : Event()

        data object RecordBatteryManually : Event()
    }
}
