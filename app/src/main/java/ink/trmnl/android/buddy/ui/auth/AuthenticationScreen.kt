package ink.trmnl.android.buddy.ui.auth

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

/**
 * Screen for authentication using device credentials (biometric or PIN/pattern/password).
 * Uses Android's native BiometricPrompt for authentication following best practices:
 * - https://developer.android.com/identity/sign-in/biometric-auth
 * - https://medium.com/androiddevelopers/migrating-from-fingerprintmanager-to-biometricprompt-4bc5f570dccd
 */
@Parcelize
data object AuthenticationScreen : Screen {
    data class State(
        val isAuthenticationAvailable: Boolean = false,
        val showRetryPrompt: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object AuthenticateRequested : Event()

        data object CancelAuthentication : Event()
    }
}
