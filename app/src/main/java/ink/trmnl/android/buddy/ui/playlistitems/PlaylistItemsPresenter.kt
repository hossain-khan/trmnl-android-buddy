package ink.trmnl.android.buddy.ui.playlistitems

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.PlaylistItem
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Presenter for PlaylistItemsScreen.
 * Fetches playlist items from API and manages state.
 */
@Inject
class PlaylistItemsPresenter
    constructor(
        @Assisted private val screen: PlaylistItemsScreen,
        @Assisted private val navigator: Navigator,
        private val apiService: TrmnlApiService,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<PlaylistItemsScreen.State> {
        @Composable
        override fun present(): PlaylistItemsScreen.State {
            var items by rememberRetained { mutableStateOf<List<PlaylistItem>>(emptyList()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            var shouldRefresh by remember { mutableStateOf(0) }

            // Load playlist items
            LaunchedEffect(shouldRefresh) {
                isLoading = true
                errorMessage = null

                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                if (preferences.apiToken.isNullOrEmpty()) {
                    errorMessage = "API token not found"
                    isLoading = false
                    return@LaunchedEffect
                }

                val authHeader = "Bearer ${preferences.apiToken}"
                when (val result = apiService.getPlaylistItems(authHeader)) {
                    is ApiResult.Success -> {
                        // Client-side filtering by device ID if specified
                        val allItems = result.value.data
                        items =
                            screen.deviceId?.let { deviceId ->
                                allItems.filter { it.deviceId == deviceId }
                            } ?: allItems

                        errorMessage = null
                        Timber.d("Loaded ${items.size} playlist items for device ${screen.deviceId ?: "all"}")
                    }
                    is ApiResult.Failure.HttpFailure -> {
                        val statusCode = result.code
                        errorMessage =
                            when (statusCode) {
                                401 -> "Unauthorized. Please check your API token."
                                404 -> "Playlist items not found."
                                else -> "HTTP error: $statusCode"
                            }
                        Timber.e("HTTP error loading playlist items: $statusCode")
                    }
                    is ApiResult.Failure.NetworkFailure -> {
                        errorMessage = "Network error. Please check your connection."
                        Timber.e(result.error, "Network error loading playlist items")
                    }
                    is ApiResult.Failure.ApiFailure -> {
                        errorMessage = "API error: ${result.error}"
                        Timber.e("API error loading playlist items: ${result.error}")
                    }
                    is ApiResult.Failure.UnknownFailure -> {
                        errorMessage = "Unexpected error occurred"
                        Timber.e(result.error, "Unknown error loading playlist items")
                    }
                }

                isLoading = false
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
