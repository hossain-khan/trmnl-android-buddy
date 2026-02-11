package ink.trmnl.android.buddy.ui.playlistitems

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.api.models.PlaylistItem
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying playlist items/content queue information.
 *
 * Shows read-only information about what content is being displayed on a TRMNL device,
 * including plugin details, rotation strategy, and rendering status.
 *
 * This screen provides visibility into the device's content queue without allowing
 * users to manage or modify the playlist items.
 */
@Parcelize
data class PlaylistItemsScreen(
    val deviceId: Int?,
    val deviceName: String,
) : Screen {
    /**
     * UI state for the Playlist Items Screen.
     *
     * @property deviceId ID of the device, null for all devices
     * @property deviceName Name of the device being displayed
     * @property items List of playlist items to display
     * @property isLoading Whether data is being loaded from API
     * @property errorMessage Error message if API call failed
     * @property eventSink Event handler for user interactions
     */
    data class State(
        val deviceId: Int?,
        val deviceName: String,
        val items: List<PlaylistItem> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Events that can be triggered from the Playlist Items Screen UI.
     */
    sealed class Event : CircuitUiEvent {
        /**
         * User clicked the back button.
         */
        data object BackClicked : Event()

        /**
         * User pulled to refresh the playlist items.
         */
        data object Refresh : Event()

        /**
         * User clicked on a playlist item for more details.
         */
        data class ItemClicked(
            val item: PlaylistItem,
        ) : Event()
    }
}
