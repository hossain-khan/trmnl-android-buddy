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
    /**
     * UI state for the authentication screen.
     *
     * **State Properties:**
     * - **isAuthenticationAvailable**: Indicates if biometric authentication (fingerprint, face, etc.)
     *   is available and configured on the device. If false, user cannot authenticate.
     * - **showRetryPrompt**: Controls visibility of retry prompt after authentication failure or
     *   cancellation. Allows user to try authentication again or bypass security.
     * - **eventSink**: Handler for user interaction events (authenticate button, cancel button).
     *
     * **State Transitions:**
     * - Initial: `isAuthenticationAvailable=false, showRetryPrompt=false`
     * - After availability check: `isAuthenticationAvailable=true/false`
     * - After auth error/cancel: `showRetryPrompt=true`
     * - After successful auth: Navigation to main screen (state cleared)
     */
    data class State(
        val isAuthenticationAvailable: Boolean = false,
        val showRetryPrompt: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Events that can be triggered from the authentication screen UI.
     *
     * **Event Flow:**
     * - User taps "Authenticate" button → [AuthenticateRequested]
     * - System shows biometric prompt → (handled by BiometricAuthHelper)
     * - On success → Navigate to main screen
     * - On failure/cancel → showRetryPrompt = true
     * - User taps "Cancel" on retry prompt → [CancelAuthentication]
     * - On cancel → Disable security preference, navigate to main screen
     *
     * @see AuthenticationPresenter Handles event processing and state updates
     */
    sealed class Event : CircuitUiEvent {
        /**
         * User requested authentication by tapping the authenticate button.
         * Triggers the system biometric prompt (fingerprint, face, PIN/pattern/password).
         */
        data object AuthenticateRequested : Event()

        /**
         * User cancelled authentication flow by tapping the cancel button on retry prompt.
         * Disables security setting and navigates to main screen without authentication.
         */
        data object CancelAuthentication : Event()
    }
}
