package ink.trmnl.android.buddy.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.security.BiometricAuthHelper
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.launch
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

/**
 * Presenter for AuthenticationScreen.
 */
@Inject
class AuthenticationPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<AuthenticationScreen.State> {
        @Composable
        override fun present(): AuthenticationScreen.State {
            var isAuthenticationAvailable by remember { mutableStateOf(false) }
            var showRetryPrompt by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            // Check authentication availability
            LaunchedEffect(Unit) {
                val biometricHelper = BiometricAuthHelper(context)
                isAuthenticationAvailable = biometricHelper.isBiometricAvailable()
            }

            return AuthenticationScreen.State(
                isAuthenticationAvailable = isAuthenticationAvailable,
                showRetryPrompt = showRetryPrompt,
            ) { event ->
                when (event) {
                    is AuthenticationScreen.Event.AuthenticateRequested -> {
                        val activity = context as? FragmentActivity
                        if (activity != null && isAuthenticationAvailable) {
                            val biometricHelper = BiometricAuthHelper(context)
                            biometricHelper.authenticate(
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

/**
 * UI content for AuthenticationScreen.
 */
@CircuitInject(AuthenticationScreen::class, AppScope::class)
@Composable
fun AuthenticationContent(
    state: AuthenticationScreen.State,
    modifier: Modifier = Modifier,
) {
    // Note: BiometricPrompt requires explicit user interaction (button click) per Android guidelines
    // We cannot auto-trigger authenticate() - user must click the button first
    // See: https://developer.android.com/identity/sign-in/biometric-auth

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isAuthenticationAvailable) {
                AuthenticationCard(
                    showRetryPrompt = state.showRetryPrompt,
                    onAuthenticateClick = {
                        state.eventSink(AuthenticationScreen.Event.AuthenticateRequested)
                    },
                    onCancelClick = {
                        state.eventSink(AuthenticationScreen.Event.CancelAuthentication)
                    },
                )
            } else {
                NoAuthenticationAvailableCard(
                    onDisableSecurity = {
                        state.eventSink(AuthenticationScreen.Event.CancelAuthentication)
                    },
                )
            }
        }
    }
}

/**
 * Card shown when authentication is available.
 * Following Android guidelines: BiometricPrompt requires explicit user interaction (button click).
 */
@Composable
private fun AuthenticationCard(
    showRetryPrompt: Boolean,
    onAuthenticateClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.fingerprint_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = "Authentication",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Authentication Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Use your fingerprint, face, or device PIN to unlock your TRMNL dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Always show authenticate button - BiometricPrompt requires user interaction
            Button(
                onClick = onAuthenticateClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.fingerprint_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Unlock")
            }

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text("Disable Security")
            }
        }
    }
}

/**
 * Card shown when no authentication method is available on the device.
 */
@Composable
private fun NoAuthenticationAvailableCard(
    onDisableSecurity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.password_2_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = "No authentication available",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            Text(
                text = "No Authentication Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text =
                    "Your device doesn't have a screen lock (PIN, pattern, password, or biometric) set up. " +
                        "Please set up a screen lock in your device settings to use this feature.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDisableSecurity,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text("Disable Security")
            }
        }
    }
}

// ========== Previews ==========

@Preview(name = "Authentication Card - Initial")
@Composable
private fun AuthenticationCardPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationCard(
            showRetryPrompt = false,
            onAuthenticateClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "Authentication Card - Retry")
@Composable
private fun AuthenticationCardRetryPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationCard(
            showRetryPrompt = true,
            onAuthenticateClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "No Authentication Available")
@Composable
private fun NoAuthenticationAvailableCardPreview() {
    TrmnlBuddyAppTheme {
        NoAuthenticationAvailableCard(
            onDisableSecurity = {},
        )
    }
}

@Preview(name = "Full Screen - Available")
@Composable
private fun AuthenticationContentPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isAuthenticationAvailable = true,
                    showRetryPrompt = false,
                ),
        )
    }
}

@Preview(name = "Full Screen - Retry")
@Composable
private fun AuthenticationContentRetryPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isAuthenticationAvailable = true,
                    showRetryPrompt = true,
                ),
        )
    }
}

@Preview(name = "Full Screen - Not Available")
@Composable
private fun AuthenticationContentNotAvailablePreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isAuthenticationAvailable = false,
                    showRetryPrompt = false,
                ),
        )
    }
}
