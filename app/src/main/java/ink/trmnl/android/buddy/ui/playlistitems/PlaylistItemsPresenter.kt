package ink.trmnl.android.buddy.ui.playlistitems

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import timber.log.Timber

/**
 * Presenter for PlaylistItemsScreen.
 *
 * **Responsibilities:**
 * - Fetches playlist items via repository (with caching)
 * - Filters by device ID when specified
 * - Manages loading, error, and success states
 * - Handles user events (back, refresh, item clicks)
 *
 * **Architecture:**
 * Uses [PlaylistItemsRepository] which caches all playlist items and provides
 * device-specific filtering. This eliminates redundant API calls when viewing
 * multiple device detail screens.
 *
 * @property screen Screen configuration with device ID and name
 * @property navigator Circuit navigator for screen transitions
 * @property repository Repository providing cached playlist items
 */
@Inject
class PlaylistItemsPresenter
    constructor(
        @Assisted private val screen: PlaylistItemsScreen,
        @Assisted private val navigator: Navigator,
        private val repository: PlaylistItemsRepository,
    ) : Presenter<PlaylistItemsScreen.State> {
        @Composable
        override fun present(): PlaylistItemsScreen.State {
            var items by rememberRetained { mutableStateOf<List<PlaylistItemUi>>(emptyList()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            var shouldRefresh by remember { mutableStateOf(0) }
            var toggleRequest by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

            // Load playlist items via repository (benefits from caching)
            LaunchedEffect(shouldRefresh) {
                isLoading = true
                errorMessage = null

                // Fetch items for specific device or all devices
                val result =
                    screen.deviceId?.let { deviceId ->
                        repository.getPlaylistItemsForDevice(
                            deviceId = deviceId,
                            forceRefresh = shouldRefresh > 0, // Force refresh on manual refresh
                        )
                    } ?: repository.getPlaylistItems(forceRefresh = shouldRefresh > 0)

                result.fold(
                    onSuccess = { fetchedItems ->
                        items = fetchedItems
                        errorMessage = null
                        Timber.d(
                            "Loaded ${items.size} playlist items for device ${screen.deviceId ?: "all"}",
                        )
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "Failed to load playlist items"
                        Timber.e(error, "Error loading playlist items")
                    },
                )

                isLoading = false
            }

            // Subscribe to repository's itemsFlow to receive cache updates
            // (e.g., when visibility toggle succeeds)
            val allItems by repository.itemsFlow.collectAsState()
            LaunchedEffect(allItems) {
                // Filter items based on device ID if specified
                items =
                    screen.deviceId?.let { deviceId ->
                        allItems.filter { it.deviceId == deviceId }
                    } ?: allItems
                Timber.d("Updated items from repository flow: ${items.size} items")
            }

            // Handle visibility toggle requests asynchronously
            LaunchedEffect(toggleRequest) {
                val request = toggleRequest ?: return@LaunchedEffect
                try {
                    repository
                        .updatePlaylistItemVisibility(
                            itemId = request.first,
                            visible = request.second,
                        ).fold(
                            onSuccess = {
                                Timber.d("Successfully toggled item ${request.first} visibility to ${request.second}")
                            },
                            onFailure = { error ->
                                val errorMsg = "Failed to toggle visibility: ${error.message}"
                                Timber.e(error, errorMsg)
                                // Propagate error message to UI for user feedback
                                errorMessage = errorMsg
                            },
                        )
                } finally {
                    // Clear the request so identical future toggles can retrigger this effect
                    toggleRequest = null
                }
            }

            return PlaylistItemsScreen.State(
                deviceId = screen.deviceId,
                deviceName = screen.deviceName,
                items = items,
                isLoading = isLoading,
                errorMessage = errorMessage,
                eventSink = { event ->
                    when (event) {
                        PlaylistItemsScreen.Event.BackClicked -> {
                            navigator.pop()
                        }
                        PlaylistItemsScreen.Event.Refresh -> {
                            shouldRefresh++
                        }
                        is PlaylistItemsScreen.Event.ItemClicked -> {
                            // TODO: Navigate to item detail screen if needed in future phases
                            Timber.d("Item clicked: ${event.item.id}")
                        }
                        is PlaylistItemsScreen.Event.ToggleItemVisibility -> {
                            // Trigger visibility toggle via state change
                            toggleRequest = event.itemId to event.newVisibility
                        }
                    }
                },
            )
        }

        @CircuitInject(PlaylistItemsScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: PlaylistItemsScreen,
                navigator: Navigator,
            ): PlaylistItemsPresenter
        }
    }
