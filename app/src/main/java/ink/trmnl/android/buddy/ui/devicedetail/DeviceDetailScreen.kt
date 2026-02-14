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
    /**
     * UI state for the device detail screen.
     *
     * **Core Device Info:**
     * - Device identification and current hardware status (passed from parent screen)
     * - Includes battery, WiFi, refresh rate, and device tokens
     *
     * **Battery Analytics:**
     * - Historical data loaded from local Room database
     * - Battery health trajectory analysis (improving/declining/stable)
     * - Automatic history clearing when anomalies detected
     * - Manual recording capability for data collection
     *
     * **Playlist Integration:**
     * - Real-time statistics from repository cache
     * - Currently playing item (most recent renderedAt timestamp)
     * - Total item count for this device
     * - Reactive updates when cache changes
     *
     * **User Preferences:**
     * - Battery tracking enabled/disabled
     * - Low battery notification settings
     * - Device token availability for preview access
     *
     * @property deviceId Unique friendly device ID
     * @property deviceName User-assigned device name
     * @property currentBattery Current battery percentage (0.0-100.0)
     * @property currentVoltage Current battery voltage (nullable)
     * @property wifiStrength WiFi signal percentage (0.0-100.0)
     * @property rssi Raw WiFi signal in dBm (nullable)
     * @property refreshRate Display refresh interval in seconds (nullable)
     * @property batteryHistory Historical battery readings from database
     * @property isLoading Loading state for initial data fetch
     * @property isBatteryTrackingEnabled User preference for battery tracking
     * @property hasRecordedToday Whether battery was manually recorded today
     * @property hasDeviceToken Whether device has API token configured
     * @property clearHistoryReason Reason to suggest clearing history (nullable)
     * @property isLowBatteryNotificationEnabled Notification preference
     * @property lowBatteryThresholdPercent Threshold for low battery alerts
     * @property isPlaylistItemsLoading Loading state for playlist prefetch
     * @property playlistItemsCount Total items for this device
     * @property nowPlayingItem Currently displayed item name
     * @property upNextItem Next item in queue
     * @property eventSink Handler for user interaction events
     */
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

    /**
     * Events that can be triggered from the device detail screen UI.
     *
     * **Navigation Events:**
     * - [BackClicked]: Return to previous screen
     * - [SettingsClicked]: Navigate to device token settings
     * - [ViewPlaylistItems]: Navigate to playlist items screen
     *
     * **Battery Management Events:**
     * - [PopulateBatteryHistory]: Generate test data for development/testing
     * - [ClearBatteryHistory]: Remove all historical data for this device
     * - [RecordBatteryManually]: Add current battery reading to history
     *
     * **Event Handling:**
     * All events are processed by [DeviceDetailPresenter] which updates state
     * or triggers navigation accordingly. Battery events interact with the
     * local Room database through [BatteryHistoryRepository].
     *
     * @see DeviceDetailPresenter Handles event processing and state management
     */
    sealed class Event : CircuitUiEvent {
        /**
         * User tapped the back button. Navigates to previous screen.
         */
        data object BackClicked : Event()

        /**
         * User tapped the settings button. Opens device token configuration.
         */
        data object SettingsClicked : Event()

        /**
         * User tapped to view playlist items. Opens full playlist items screen.
         */
        data object ViewPlaylistItems : Event()

        /**
         * Developer action to populate battery history with test data.
         * Creates synthetic battery readings with declining trend.
         *
         * @property minBatteryLevel Minimum battery level for generated data (0.0-100.0)
         */
        data class PopulateBatteryHistory(
            val minBatteryLevel: Float,
        ) : Event()

        /**
         * User confirmed clearing all battery history for this device.
         * Removes all historical readings from local database.
         */
        data object ClearBatteryHistory : Event()

        /**
         * User tapped to manually record current battery reading.
         * Adds current battery level and voltage to historical data.
         */
        data object RecordBatteryManually : Event()
    }
}
