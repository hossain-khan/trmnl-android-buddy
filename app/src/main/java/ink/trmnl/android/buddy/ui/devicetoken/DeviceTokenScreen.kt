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
import androidx.compose.ui.unit.dp
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
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
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
            var currentToken by remember { mutableStateOf("") }
            var tokenInput by remember { mutableStateOf("") }
            var isSaving by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // Load existing token on initial load
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
                        if (tokenInput.isBlank()) {
                            errorMessage = "Token cannot be empty"
                            return@State
                        }

                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                deviceTokenRepository.saveDeviceToken(screen.deviceFriendlyId, tokenInput.trim())
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
                title = { Text("Device API Key") },
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
            // Device Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                                state.deviceName,
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
                                state.deviceFriendlyId,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                    )
                }
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        "Format: abc-123",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Token Input
            OutlinedTextField(
                value = state.tokenInput,
                onValueChange = { state.eventSink(DeviceTokenScreen.Event.TokenChanged(it)) },
                label = { Text("Device API Key") },
                placeholder = { Text("abc-123") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                isError = state.errorMessage != null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
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
                    if (state.errorMessage != null) {
                        Text(
                            state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (state.currentToken.isNotEmpty()) {
                        Text("Current token is set. You can update or clear it.")
                    }
                },
                singleLine = true,
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Save Button
                Button(
                    onClick = { state.eventSink(DeviceTokenScreen.Event.SaveToken) },
                    enabled = !state.isSaving && state.tokenInput.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.currentToken.isEmpty()) "Save" else "Update")
                }

                // Clear Button (only show if token exists)
                if (state.currentToken.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { state.eventSink(DeviceTokenScreen.Event.ClearToken) },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Clear")
                    }
                }
            }

            // Cancel Button
            TextButton(
                onClick = { state.eventSink(DeviceTokenScreen.Event.BackClicked) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        }
    }
}
