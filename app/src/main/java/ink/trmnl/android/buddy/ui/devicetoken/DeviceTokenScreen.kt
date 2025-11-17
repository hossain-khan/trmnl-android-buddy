package ink.trmnl.android.buddy.ui.devicetoken

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

/**
 * Screen for managing Device API Key (Access Token) for a specific device.
 *
 * Allows users to:
 * - View device information (name, ID)
 * - Set/update device API key
 * - Clear existing API key
 */
@Parcelize
data class DeviceTokenScreen(
    val deviceFriendlyId: String,
    val deviceName: String,
) : Screen {
    data class State(
        val deviceFriendlyId: String = "",
        val deviceName: String = "",
        val currentToken: String = "",
        val tokenInput: String = "",
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class TokenChanged(
            val token: String,
        ) : Event()

        data object SaveToken : Event()

        data object ClearToken : Event()

        data object BackClicked : Event()
    }
}
