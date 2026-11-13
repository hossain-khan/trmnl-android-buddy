package ink.trmnl.android.buddy.ui.accesstoken

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import kotlinx.parcelize.Parcelize

/**
 * Screen for entering and saving TRMNL API token.
 */
@Parcelize
data object AccessTokenScreen : ParcelableScreen {
    data class State(
        val token: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isLegacyToken: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class TokenChanged(
            val token: String,
        ) : Event

        data object SaveClicked : Event

        data object BackClicked : Event
    }
}
