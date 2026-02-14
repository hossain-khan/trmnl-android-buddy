package ink.trmnl.android.buddy.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.security.BiometricAuthHelper
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import kotlinx.coroutines.launch

/**
 * Presenter for [AuthenticationScreen].
 *
 * **Responsibilities:**
 * - Checks device biometric authentication availability (fingerprint/face unlock)
 * - Triggers biometric authentication prompt on user request
 * - Handles authentication success, error, and cancellation scenarios
 * - Manages retry prompts when authentication fails
 * - Allows users to bypass security if preferred
 *
 * **Authentication Flow:**
 * 1. On screen load: Checks if biometric authentication is available on device
 * 2. User taps authenticate button: Shows system biometric prompt
 * 3. On success: Navigates to [TrmnlDevicesScreen] (main app screen)
 * 4. On error/cancel: Shows retry prompt allowing user to try again or cancel
 * 5. On cancel security: Disables security setting and proceeds to main screen
 *
 * **State Retention:**
 * Uses `remember` (not `rememberRetained`) because:
 * - This is a one-time authentication gate screen
 * - State should reset if user navigates back (e.g., from settings)
 * - No benefit to retaining authentication state across back stack
 *
 * @property navigator Circuit navigator for screen transitions
 * @property userPreferencesRepository Repository for reading/updating security preferences
 * @property biometricAuthHelper Helper for triggering biometric authentication
 *
 * @see AuthenticationScreen Screen definition with State and Event sealed classes
 * @see BiometricAuthHelper Android biometric authentication wrapper
 */
@Inject
class AuthenticationPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val biometricAuthHelper: BiometricAuthHelper,
    ) : Presenter<AuthenticationScreen.State> {
        @Composable
        override fun present(): AuthenticationScreen.State {
            var isAuthenticationAvailable by remember { mutableStateOf(false) }
            var showRetryPrompt by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            // Check authentication availability
            LaunchedEffect(Unit) {
                isAuthenticationAvailable = biometricAuthHelper.isBiometricAvailable()
            }

            return AuthenticationScreen.State(
                isAuthenticationAvailable = isAuthenticationAvailable,
                showRetryPrompt = showRetryPrompt,
            ) { event ->
                when (event) {
                    is AuthenticationScreen.Event.AuthenticateRequested -> {
                        val activity = context as FragmentActivity
                        biometricAuthHelper.authenticate(
                            activity = activity,
                            title = "Authenticate to continue",
                            subtitle = "Unlock to access your TRMNL dashboard",
                            onSuccess = {
                                navigator.resetRoot(TrmnlDevicesScreen)
                            },
                            onError = { error ->
                                // Show retry prompt on error
                                showRetryPrompt = true
                            },
                            onUserCancelled = {
                                // User cancelled, show retry prompt
                                showRetryPrompt = true
                            },
                        )
                    }

                    AuthenticationScreen.Event.CancelAuthentication -> {
                        coroutineScope.launch {
                            // Disable security and navigate to devices screen
                            userPreferencesRepository.setSecurityEnabled(false)
                            navigator.resetRoot(TrmnlDevicesScreen)
                        }
                    }
                }
            }
        }

        @CircuitInject(AuthenticationScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): AuthenticationPresenter
        }
    }
