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
import ink.trmnl.android.buddy.data.PlaylistItemsRepository
import ink.trmnl.android.buddy.data.getCurrentlyPlayingItem
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.devicetoken.DeviceTokenScreen
import ink.trmnl.android.buddy.ui.playlistitems.PlaylistItemsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Presenter for [DeviceDetailScreen] managing playlist and preference concerns.
 *
 * **Responsibilities:**
 * - Monitors playlist items for the device (currently playing, total count)
 * - Manages low battery notification preference display
 * - Checks device token availability
 * - Coordinates navigation to playlist items and device settings
 *
 * **Battery Analytics (extracted):**
 * Battery history loading, analysis, recording and clearing are now handled by
 * the separate [BatteryChartPresenter], embedded via `CircuitContent` in [DeviceDetailContent].
 * This separation improves testability and adheres to single-responsibility principle.
 *
 * **Data Sources:**
 * - **Playlist items**: Repository cache (shared across app), prefetched from API
 * - **User preferences**: DataStore for notification config
 * - **Device token**: Local storage for device-specific API access
 *
 * **Smart State Management:**
 * - Uses `rememberRetained` for UI state (survives config changes and back stack)
 * - Uses `collectAsState` for reactive Flow subscriptions (repository cache)
 * - Uses `derivedStateOf` for computed playlist stats
 *
 * **Playlist Items Integration:**
 * Subscribes to [PlaylistItemsRepository.itemsFlow] to reactively update:
 * - Total playlist items count for this device
 * - Currently playing item (item with most recent renderedAt timestamp)
 * - Next item in queue (next visible item by row order)
 * This enables real-time updates when items are added, removed, or reordered.
 *
 * @property screen Screen parameters with device identification and current status
 * @property navigator Circuit navigator for screen transitions
 * @property userPreferencesRepository Repository for user settings and preferences
 * @property deviceTokenRepository Repository for device-specific API tokens
 * @property playlistItemsRepository Repository with cached playlist items
 *
 * @see DeviceDetailScreen Screen definition with State and Event sealed classes
 * @see BatteryChartPresenter Separate presenter for battery history management
 * @see PlaylistItemsRepository Shared repository with intelligent caching
 */
@Inject
class DeviceDetailPresenter
    constructor(
        @Assisted private val screen: DeviceDetailScreen,
        @Assisted private val navigator: Navigator,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val deviceTokenRepository: DeviceTokenRepository,
        private val playlistItemsRepository: PlaylistItemsRepository,
    ) : Presenter<DeviceDetailScreen.State> {
        @Composable
        override fun present(): DeviceDetailScreen.State {
            val allPlaylistItems by playlistItemsRepository.itemsFlow.collectAsState()
            var isPlaylistItemsLoading by rememberRetained { mutableStateOf(true) }
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

            // Prefetch playlist items for this device and cache them
            LaunchedEffect(screen.deviceNumericId) {
                if (screen.deviceNumericId != null) {
                    isPlaylistItemsLoading = true
                    try {
                        withContext(Dispatchers.IO) {
                            playlistItemsRepository.getPlaylistItemsForDevice(screen.deviceNumericId)
                        }
                    } catch (e: Exception) {
                        // Silently handle error - playlist items will load when user clicks "View"
                    } finally {
                        // Cache is now populated (or failed), loading complete
                        isPlaylistItemsLoading = false
                    }
                } else {
                    // No numeric device ID; nothing to prefetch, ensure loading state is cleared
                    isPlaylistItemsLoading = false
                }
            }

            // Calculate playlist items stats for this device
            val playlistItemsStats by remember {
                derivedStateOf {
                    if (screen.deviceNumericId != null) {
                        val deviceItems = allPlaylistItems.filter { it.deviceId == screen.deviceNumericId }
                        val count = deviceItems.size
                        // Use the utility function to find currently playing item
                        val nowPlaying = getCurrentlyPlayingItem(deviceItems)
                        // Find up next item (next visible item after currently playing)
                        val upNext =
                            if (nowPlaying != null) {
                                val currentIndex = deviceItems.indexOfFirst { it.id == nowPlaying.id }
                                if (currentIndex >= 0) {
                                    // Find the next visible item after currently playing
                                    deviceItems
                                        .drop(currentIndex + 1)
                                        .firstOrNull { it.isVisible }
                                        ?.displayName ?: ""
                                } else {
                                    ""
                                }
                            } else {
                                ""
                            }
                        Triple(count, nowPlaying?.displayName ?: "", upNext)
                    } else {
                        Triple(0, "", "")
                    }
                }
            }
            val playlistItemsCount = playlistItemsStats.first
            val nowPlayingItem = playlistItemsStats.second
            val upNextItem = playlistItemsStats.third

            return DeviceDetailScreen.State(
                deviceId = screen.deviceId,
                deviceName = screen.deviceName,
                currentBattery = screen.currentBattery,
                currentVoltage = screen.currentVoltage,
                wifiStrength = screen.wifiStrength,
                rssi = screen.rssi,
                refreshRate = screen.refreshRate,
                isLowBatteryNotificationEnabled = isLowBatteryNotificationEnabled,
                lowBatteryThresholdPercent = lowBatteryThresholdPercent,
                hasDeviceToken = hasDeviceToken,
                isPlaylistItemsLoading = isPlaylistItemsLoading,
                playlistItemsCount = playlistItemsCount,
                nowPlayingItem = nowPlayingItem,
                upNextItem = upNextItem,
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
                    DeviceDetailScreen.Event.ViewPlaylistItems -> {
                        navigator.goTo(
                            PlaylistItemsScreen(
                                deviceId = screen.deviceNumericId,
                                deviceName = screen.deviceName,
                            ),
                        )
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
