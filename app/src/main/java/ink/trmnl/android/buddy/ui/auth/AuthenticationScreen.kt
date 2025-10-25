package ink.trmnl.android.buddy.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
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
import ink.trmnl.android.buddy.security.SecurityHelper
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * Screen for authentication (PIN entry or biometric).
 * Shown when user has enabled security and needs to authenticate to access the app.
 */
@Parcelize
data object AuthenticationScreen : Screen {
    data class State(
        val isSetupMode: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val isBiometricEnabled: Boolean = false,
        val errorMessage: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class PinEntered(
            val pin: String,
        ) : Event()

        data object BiometricAuthRequested : Event()

        data class SetupPin(
            val pin: String,
            val confirmPin: String,
        ) : Event()

        data object CancelSetup : Event()

        data object DismissError : Event()
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
            var isSetupMode by rememberRetained { mutableStateOf(false) }
            var isBiometricAvailable by rememberRetained { mutableStateOf(false) }
            var isBiometricEnabled by rememberRetained { mutableStateOf(false) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            // Check biometric availability and preferences
            LaunchedEffect(Unit) {
                val biometricHelper = BiometricAuthHelper(context)
                isBiometricAvailable = biometricHelper.isBiometricAvailable()

                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                isBiometricEnabled = preferences.isBiometricEnabled

                // Check if this is setup mode (no PIN set yet)
                isSetupMode = preferences.pinHash == null
            }

            return AuthenticationScreen.State(
                isSetupMode = isSetupMode,
                isBiometricAvailable = isBiometricAvailable,
                isBiometricEnabled = isBiometricEnabled,
                errorMessage = errorMessage,
            ) { event ->
                when (event) {
                    is AuthenticationScreen.Event.PinEntered -> {
                        coroutineScope.launch {
                            val preferences = userPreferencesRepository.userPreferencesFlow.first()
                            val storedHash = preferences.pinHash

                            if (storedHash != null && SecurityHelper.verifyPin(event.pin, storedHash)) {
                                // PIN verified, navigate to devices screen
                                navigator.resetRoot(TrmnlDevicesScreen)
                            } else {
                                errorMessage = "Incorrect PIN. Please try again."
                            }
                        }
                    }

                    is AuthenticationScreen.Event.BiometricAuthRequested -> {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            val biometricHelper = BiometricAuthHelper(context)
                            biometricHelper.authenticate(
                                activity = activity,
                                title = "Authenticate to continue",
                                subtitle = "Use your biometric to unlock",
                                onSuccess = {
                                    navigator.resetRoot(TrmnlDevicesScreen)
                                },
                                onError = { error ->
                                    errorMessage = error
                                },
                            )
                        }
                    }

                    is AuthenticationScreen.Event.SetupPin -> {
                        if (event.pin != event.confirmPin) {
                            errorMessage = "PINs do not match"
                        } else if (!SecurityHelper.isValidPin(event.pin)) {
                            errorMessage = "PIN must be at least ${SecurityHelper.MIN_PIN_LENGTH} digits"
                        } else {
                            coroutineScope.launch {
                                val hash = SecurityHelper.hashPin(event.pin)
                                userPreferencesRepository.setPinHash(hash)
                                userPreferencesRepository.setSecurityEnabled(true)
                                navigator.resetRoot(TrmnlDevicesScreen)
                            }
                        }
                    }

                    AuthenticationScreen.Event.CancelSetup -> {
                        coroutineScope.launch {
                            userPreferencesRepository.setSecurityEnabled(false)
                            navigator.resetRoot(TrmnlDevicesScreen)
                        }
                    }

                    AuthenticationScreen.Event.DismissError -> {
                        errorMessage = null
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
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isSetupMode) {
                PinSetupCard(
                    errorMessage = state.errorMessage,
                    onSetupPin = { pin, confirmPin ->
                        state.eventSink(AuthenticationScreen.Event.SetupPin(pin, confirmPin))
                    },
                    onCancel = { state.eventSink(AuthenticationScreen.Event.CancelSetup) },
                    onDismissError = { state.eventSink(AuthenticationScreen.Event.DismissError) },
                )
            } else {
                PinEntryCard(
                    isBiometricAvailable = state.isBiometricAvailable,
                    isBiometricEnabled = state.isBiometricEnabled,
                    errorMessage = state.errorMessage,
                    onPinEntered = { pin ->
                        state.eventSink(AuthenticationScreen.Event.PinEntered(pin))
                    },
                    onBiometricAuth = {
                        state.eventSink(AuthenticationScreen.Event.BiometricAuthRequested)
                    },
                    onDismissError = { state.eventSink(AuthenticationScreen.Event.DismissError) },
                )
            }
        }
    }
}

/**
 * Card for PIN setup (first time setup).
 */
@Composable
private fun PinSetupCard(
    errorMessage: String?,
    onSetupPin: (String, String) -> Unit,
    onCancel: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.password_2_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = "Set up PIN",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Set up PIN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Create a ${SecurityHelper.MIN_PIN_LENGTH}-digit PIN to secure your TRMNL dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        pin = it
                    }
                },
                label = { Text("Enter PIN") },
                visualTransformation =
                    if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            painter =
                                painterResource(
                                    if (showPin) {
                                        R.drawable.baseline_visibility_off_24
                                    } else {
                                        R.drawable.baseline_visibility_24
                                    },
                                ),
                            contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                        )
                    }
                },
            )

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        confirmPin = it
                    }
                },
                label = { Text("Confirm PIN") },
                visualTransformation =
                    if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onDismissError()
                        onSetupPin(pin, confirmPin)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = pin.isNotEmpty() && confirmPin.isNotEmpty(),
                ) {
                    Text("Set PIN")
                }
            }
        }
    }
}

/**
 * Card for PIN entry (authentication).
 */
@Composable
private fun PinEntryCard(
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    errorMessage: String?,
    onPinEntered: (String) -> Unit,
    onBiometricAuth: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    // Auto-trigger biometric on first load if enabled
    LaunchedEffect(Unit) {
        if (isBiometricAvailable && isBiometricEnabled) {
            onBiometricAuth()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.password_2_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = "Enter PIN",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Enter PIN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Enter your PIN to access TRMNL dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        pin = it
                    }
                },
                label = { Text("PIN") },
                visualTransformation =
                    if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            painter =
                                painterResource(
                                    if (showPin) {
                                        R.drawable.baseline_visibility_off_24
                                    } else {
                                        R.drawable.baseline_visibility_24
                                    },
                                ),
                            contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                        )
                    }
                },
            )

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = {
                    onDismissError()
                    onPinEntered(pin)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.isNotEmpty(),
            ) {
                Text("Unlock")
            }

            if (isBiometricAvailable && isBiometricEnabled) {
                TextButton(onClick = onBiometricAuth) {
                    Icon(
                        painter = painterResource(R.drawable.fingerprint_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = "Use biometric",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometric")
                }
            }
        }
    }
}

// ========== Previews ==========

@Preview(name = "PIN Setup")
@Composable
private fun PinSetupCardPreview() {
    TrmnlBuddyAppTheme {
        PinSetupCard(
            errorMessage = null,
            onSetupPin = { _, _ -> },
            onCancel = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "PIN Setup - With Error")
@Composable
private fun PinSetupCardErrorPreview() {
    TrmnlBuddyAppTheme {
        PinSetupCard(
            errorMessage = "PINs do not match",
            onSetupPin = { _, _ -> },
            onCancel = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "PIN Entry")
@Composable
private fun PinEntryCardPreview() {
    TrmnlBuddyAppTheme {
        PinEntryCard(
            isBiometricAvailable = true,
            isBiometricEnabled = true,
            errorMessage = null,
            onPinEntered = {},
            onBiometricAuth = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "PIN Entry - With Error")
@Composable
private fun PinEntryCardErrorPreview() {
    TrmnlBuddyAppTheme {
        PinEntryCard(
            isBiometricAvailable = false,
            isBiometricEnabled = false,
            errorMessage = "Incorrect PIN. Please try again.",
            onPinEntered = {},
            onBiometricAuth = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "Authentication Screen - Setup Mode")
@Composable
private fun AuthenticationScreenSetupPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isSetupMode = true,
                    isBiometricAvailable = false,
                    isBiometricEnabled = false,
                ),
        )
    }
}

@Preview(name = "Authentication Screen - Entry Mode")
@Composable
private fun AuthenticationScreenEntryPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isSetupMode = false,
                    isBiometricAvailable = true,
                    isBiometricEnabled = true,
                ),
        )
    }
}
