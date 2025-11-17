package ink.trmnl.android.buddy.ui.announcements

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying all announcements.
 *
 * @param isEmbedded When true, hides the top app bar (for use in ContentHubScreen)
 */
@Parcelize
data class AnnouncementsScreen(
    val isEmbedded: Boolean = false,
) : Screen {
    data class State(
        val announcements: List<AnnouncementEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val filter: Filter = Filter.ALL,
        val unreadCount: Int = 0,
        val errorMessage: String? = null,
        val showTopBar: Boolean = true, // Control whether to show top app bar
        val showAuthBanner: Boolean = false, // Control whether to show authentication banner
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object Refresh : Event()

        data class FilterChanged(
            val filter: Filter,
        ) : Event()

        data class AnnouncementClicked(
            val announcement: AnnouncementEntity,
        ) : Event()

        data class ToggleReadStatus(
            val announcement: AnnouncementEntity,
        ) : Event()

        data object MarkAllAsRead : Event()

        data object DismissAuthBanner : Event()
    }

    enum class Filter {
        ALL,
        UNREAD,
        READ,
    }
}
