package ink.trmnl.android.buddy.ui.devicedetail

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.data.battery.BatteryHistoryAnalyzer
import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying detailed information about a single TRMNL device.
 *
 * **Features:**
 * - Battery history chart with health trajectory analysis
 * - WiFi signal strength with RSSI details
 * - Refresh rate information
 * - Playlist items statistics (total count and currently playing)
 * - Manual battery data recording
 * - Battery history management (clear, populate test data)
 * - Navigation to playlist items and device settings
 *
 * **Data Requirements:**
 * This screen requires comprehensive device information passed as parameters:
 * - Device identification (ID, name, numeric ID)
 * - Current hardware status (battery, voltage, WiFi, RSSI)
 * - Configuration (refresh rate)
 *
 * The screen also loads additional data:
 * - Historical battery readings from local database
 * - Playlist items from repository cache
 * - User preferences (tracking settings, notifications)
 *
 * @property deviceId Unique friendly device ID (e.g., "abc123")
 * @property deviceName User-assigned device name
 * @property currentBattery Current battery percentage (0.0 to 100.0)
 * @property currentVoltage Current battery voltage (may be null)
 * @property wifiStrength WiFi signal strength percentage (0.0 to 100.0)
 * @property rssi Raw WiFi signal strength in dBm (may be null)
 * @property refreshRate Display refresh interval in seconds (may be null)
 * @property deviceNumericId Numeric device ID for API calls (may be null)
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
        /**
         * in seconds, null if not available
         */
        val refreshRate: Int?,
        val batteryHistory: List<BatteryHistoryEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isBatteryTrackingEnabled: Boolean = true,
        val hasRecordedToday: Boolean = false,
        val hasDeviceToken: Boolean = false,
        val clearHistoryReason: BatteryHistoryAnalyzer.ClearHistoryReason? = null,
        val isLowBatteryNotificationEnabled: Boolean = false,
        val lowBatteryThresholdPercent: Int = 20,
        /**
         * Tracks playlist items prefetch progress
         */
        val isPlaylistItemsLoading: Boolean = true,
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
