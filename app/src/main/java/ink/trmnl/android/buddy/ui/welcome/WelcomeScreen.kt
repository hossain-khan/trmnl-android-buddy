package ink.trmnl.android.buddy.ui.welcome

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

/**
 * Welcome screen - the first screen shown to users.
 * Checks if API token exists and routes accordingly.
 */
@Parcelize
data object WelcomeScreen : Screen {
    data class State(
        val isLoading: Boolean = true,
        val hasExistingToken: Boolean = false,
        val hasRecentContent: Boolean = false,
        val recentContentCount: Int = 0,
        val unreadContentCount: Int = 0,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GetStartedClicked : Event()

        data object ViewUpdatesClicked : Event()
    }
}
