package ink.trmnl.android.buddy.ui.devicetoken

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.util.PrivacyUtils
import kotlinx.coroutines.launch
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

/**
 * Presenter for DeviceTokenScreen.
 *
 * Best Practices Applied:
 * - Uses `rememberRetained` for form state
 * - LaunchedEffect with screen parameter to reload when device changes
 * - Input validation before saving
 * - Separate current token vs input token for better UX
 */
@Inject
class DeviceTokenPresenter
    constructor(
        @Assisted private val screen: DeviceTokenScreen,
        @Assisted private val navigator: Navigator,
        private val deviceTokenRepository: DeviceTokenRepository,
    ) : Presenter<DeviceTokenScreen.State> {
        @Composable
        override fun present(): DeviceTokenScreen.State {
            // State: Form state that survives configuration changes
            var currentToken by rememberRetained { mutableStateOf("") }
            var tokenInput by rememberRetained { mutableStateOf("") }
            var isSaving by rememberRetained { mutableStateOf(false) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // Side Effect: Load existing token when screen device ID changes
            LaunchedEffect(screen.deviceFriendlyId) {
                val token = deviceTokenRepository.getDeviceToken(screen.deviceFriendlyId)
                if (token != null) {
                    currentToken = token
                    tokenInput = token
                }
            }

            return DeviceTokenScreen.State(
                deviceFriendlyId = screen.deviceFriendlyId,
                deviceName = screen.deviceName,
                currentToken = currentToken,
                tokenInput = tokenInput,
                isSaving = isSaving,
                errorMessage = errorMessage,
            ) { event ->
                when (event) {
                    is DeviceTokenScreen.Event.TokenChanged -> {
                        tokenInput = event.token
                        errorMessage = null
                    }

                    DeviceTokenScreen.Event.SaveToken -> {
                        val trimmedToken = tokenInput.trim()
                        if (trimmedToken.isBlank()) {
                            errorMessage = "Token cannot be empty"
                            return@State
                        }
                        if (trimmedToken.length < 20) {
                            errorMessage = "Token must be at least 20 characters long"
                            return@State
                        }

                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                deviceTokenRepository.saveDeviceToken(screen.deviceFriendlyId, trimmedToken)
                                // Navigate back to devices list
                                navigator.pop()
                            } catch (e: Exception) {
                                errorMessage = "Failed to save token: ${e.message}"
                                isSaving = false
                            }
                        }
                    }

                    DeviceTokenScreen.Event.ClearToken -> {
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                deviceTokenRepository.clearDeviceToken(screen.deviceFriendlyId)
                                currentToken = ""
                                tokenInput = ""
                                isSaving = false
                            } catch (e: Exception) {
                                errorMessage = "Failed to clear token: ${e.message}"
                                isSaving = false
                            }
                        }
                    }

                    DeviceTokenScreen.Event.BackClicked -> {
                        navigator.pop()
                    }
                }
            }
        }

        @CircuitInject(DeviceTokenScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: DeviceTokenScreen,
                navigator: Navigator,
            ): DeviceTokenPresenter
        }
    }

/**
 * UI content for DeviceTokenScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DeviceTokenScreen::class, AppScope::class)
@Composable
fun DeviceTokenContent(
    state: DeviceTokenScreen.State,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("Device API Key") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DeviceTokenScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DeviceInfoCard(
                deviceName = state.deviceName,
                deviceFriendlyId = state.deviceFriendlyId,
            )

            InstructionsCard()

            TokenInputSection(
                tokenInput = state.tokenInput,
                currentToken = state.currentToken,
                errorMessage = state.errorMessage,
                isSaving = state.isSaving,
                passwordVisible = passwordVisible,
                onTokenChanged = { state.eventSink(DeviceTokenScreen.Event.TokenChanged(it)) },
                onPasswordVisibilityToggled = { passwordVisible = !passwordVisible },
            )

            ActionButtons(
                currentToken = state.currentToken,
                tokenInput = state.tokenInput,
                isSaving = state.isSaving,
                onSaveClicked = { state.eventSink(DeviceTokenScreen.Event.SaveToken) },
                onClearClicked = { state.eventSink(DeviceTokenScreen.Event.ClearToken) },
                onCancelClicked = { state.eventSink(DeviceTokenScreen.Event.BackClicked) },
            )
        }
    }
}

/**
 * Device information card showing device name and ID.
 */
@Composable
private fun DeviceInfoCard(
    deviceName: String,
    deviceFriendlyId: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(
                        "Device Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                supportingContent = {
                    Text(
                        deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
            ListItem(
                headlineContent = {
                    Text(
                        "Device ID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                supportingContent = {
                    Text(
                        PrivacyUtils.obfuscateDeviceId(deviceFriendlyId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}

/**
 * Instructions card explaining the Device API Key requirements.
 */
@Composable
private fun InstructionsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "About Device API Key",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "The Device API Key (Access Token) is required to fetch the current display content for this device. " +
                    "You can find this key in your device settings on the TRMNL website.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Format: 20+ character hexadecimal string (e.g., 1a2b3c4d5e6f7g8h9i0j...)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Token input section with password visibility toggle.
 */
@Composable
private fun TokenInputSection(
    tokenInput: String,
    currentToken: String,
    errorMessage: String?,
    isSaving: Boolean,
    passwordVisible: Boolean,
    onTokenChanged: (String) -> Unit,
    onPasswordVisibilityToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = tokenInput,
        onValueChange = onTokenChanged,
        label = { Text("Device API Key") },
        placeholder = { Text("1a2b3c4d5e6f7g8h9i0j...") },
        modifier = modifier.fillMaxWidth(),
        enabled = !isSaving,
        isError = errorMessage != null,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onPasswordVisibilityToggled) {
                Icon(
                    painter =
                        painterResource(
                            if (passwordVisible) {
                                R.drawable.visibility_off_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            } else {
                                R.drawable.visibility_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            },
                        ),
                    contentDescription = if (passwordVisible) "Hide token" else "Show token",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supportingText = {
            if (errorMessage != null) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (currentToken.isNotEmpty()) {
                Text("Current token is set. You can update or clear it.")
            }
        },
        singleLine = true,
    )
}

/**
 * Action buttons section including Save, Clear, and Cancel buttons.
 */
@Composable
private fun ActionButtons(
    currentToken: String,
    tokenInput: String,
    isSaving: Boolean,
    onSaveClicked: () -> Unit,
    onClearClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Save Button
            Button(
                onClick = onSaveClicked,
                enabled = !isSaving && tokenInput.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (currentToken.isEmpty()) "Save" else "Update")
            }

            // Clear Button (only show if token exists)
            if (currentToken.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearClicked,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear")
                }
            }
        }

        // Cancel Button
        TextButton(
            onClick = onCancelClicked,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel")
        }
    }
}

// ========== Previews ==========

/**
 * Preview of DeviceTokenContent in light and dark mode with empty state.
 */
@PreviewLightDark
@Composable
private fun DeviceTokenContentEmptyPreview() {
    TrmnlBuddyAppTheme {
        DeviceTokenContent(
            state =
                DeviceTokenScreen.State(
                    deviceFriendlyId = "TRMNL-ABC-123",
                    deviceName = "Living Room Display",
                    currentToken = "",
                    tokenInput = "",
                    isSaving = false,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of DeviceTokenContent with token entered.
 */
@PreviewLightDark
@Composable
private fun DeviceTokenContentWithTokenPreview() {
    TrmnlBuddyAppTheme {
        DeviceTokenContent(
            state =
                DeviceTokenScreen.State(
                    deviceFriendlyId = "TRMNL-ABC-123",
                    deviceName = "Living Room Display",
                    currentToken = "",
                    tokenInput = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p",
                    isSaving = false,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of DeviceTokenContent with existing token saved.
 */
@PreviewLightDark
@Composable
private fun DeviceTokenContentSavedTokenPreview() {
    TrmnlBuddyAppTheme {
        DeviceTokenContent(
            state =
                DeviceTokenScreen.State(
                    deviceFriendlyId = "TRMNL-ABC-123",
                    deviceName = "Living Room Display",
                    currentToken = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p",
                    tokenInput = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p",
                    isSaving = false,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of DeviceTokenContent with error state.
 */
@PreviewLightDark
@Composable
private fun DeviceTokenContentErrorPreview() {
    TrmnlBuddyAppTheme {
        DeviceTokenContent(
            state =
                DeviceTokenScreen.State(
                    deviceFriendlyId = "TRMNL-ABC-123",
                    deviceName = "Living Room Display",
                    currentToken = "",
                    tokenInput = "short",
                    isSaving = false,
                    errorMessage = "Token must be at least 20 characters long",
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of DeviceTokenContent in saving state.
 */
@Preview
@Composable
private fun DeviceTokenContentSavingPreview() {
    TrmnlBuddyAppTheme {
        DeviceTokenContent(
            state =
                DeviceTokenScreen.State(
                    deviceFriendlyId = "TRMNL-ABC-123",
                    deviceName = "Living Room Display",
                    currentToken = "",
                    tokenInput = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p",
                    isSaving = true,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of DeviceInfoCard component.
 */
@PreviewLightDark
@Composable
private fun DeviceInfoCardPreview() {
    TrmnlBuddyAppTheme {
        DeviceInfoCard(
            deviceName = "Living Room Display",
            deviceFriendlyId = "TRMNL-ABC-123",
        )
    }
}

/**
 * Preview of InstructionsCard component.
 */
@PreviewLightDark
@Composable
private fun InstructionsCardPreview() {
    TrmnlBuddyAppTheme {
        InstructionsCard()
    }
}

/**
 * Preview of TokenInputSection component in empty state.
 */
@PreviewLightDark
@Composable
private fun TokenInputSectionEmptyPreview() {
    TrmnlBuddyAppTheme {
        TokenInputSection(
            tokenInput = "",
            currentToken = "",
            errorMessage = null,
            isSaving = false,
            passwordVisible = false,
            onTokenChanged = {},
            onPasswordVisibilityToggled = {},
        )
    }
}

/**
 * Preview of TokenInputSection component with error.
 */
@PreviewLightDark
@Composable
private fun TokenInputSectionErrorPreview() {
    TrmnlBuddyAppTheme {
        TokenInputSection(
            tokenInput = "short",
            currentToken = "",
            errorMessage = "Token must be at least 20 characters long",
            isSaving = false,
            passwordVisible = false,
            onTokenChanged = {},
            onPasswordVisibilityToggled = {},
        )
    }
}

/**
 * Preview of ActionButtons component with no existing token.
 */
@PreviewLightDark
@Composable
private fun ActionButtonsEmptyPreview() {
    TrmnlBuddyAppTheme {
        ActionButtons(
            currentToken = "",
            tokenInput = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p",
            isSaving = false,
            onSaveClicked = {},
            onClearClicked = {},
            onCancelClicked = {},
        )
    }
}

/**
 * Preview of ActionButtons component with existing token.
 */
@PreviewLightDark
@Composable
private fun ActionButtonsWithTokenPreview() {
    TrmnlBuddyAppTheme {
        ActionButtons(
            currentToken = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p",
            tokenInput = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p",
            isSaving = false,
            onSaveClicked = {},
            onClearClicked = {},
            onCancelClicked = {},
        )
    }
}
