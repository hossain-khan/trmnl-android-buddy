package ink.trmnl.android.buddy.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying list of TRMNL devices.
 * Shows device status, battery level, and WiFi strength.
 */
@Parcelize
data object TrmnlDevicesScreen : Screen {
    data class State(
        val devices: List<Device> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object Refresh : Event()

        data object AccountClicked : Event()

        data class DeviceClicked(
            val device: Device,
        ) : Event()
    }
}

/**
 * Presenter for TrmnlDevicesScreen.
 * Fetches devices from API and manages state.
 */
@Inject
class TrmnlDevicesPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val apiService: TrmnlApiService,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<TrmnlDevicesScreen.State> {
        @Composable
        override fun present(): TrmnlDevicesScreen.State {
            var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // Fetch devices on initial load
            LaunchedEffect(Unit) {
                loadDevices(
                    onSuccess = { fetchedDevices ->
                        devices = fetchedDevices
                        isLoading = false
                    },
                    onError = { error ->
                        errorMessage = error
                        isLoading = false
                    },
                )
            }

            return TrmnlDevicesScreen.State(
                devices = devices,
                isLoading = isLoading,
                errorMessage = errorMessage,
            ) { event ->
                when (event) {
                    TrmnlDevicesScreen.Event.Refresh -> {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            loadDevices(
                                onSuccess = { fetchedDevices ->
                                    devices = fetchedDevices
                                    isLoading = false
                                },
                                onError = { error ->
                                    errorMessage = error
                                    isLoading = false
                                },
                            )
                        }
                    }

                    is TrmnlDevicesScreen.Event.AccountClicked -> {
                        navigator.goTo(ink.trmnl.android.buddy.ui.user.UserAccountScreen)
                    }

                    is TrmnlDevicesScreen.Event.DeviceClicked -> {
                        // TODO: Navigate to device detail screen
                        // navigator.goTo(DeviceDetailScreen(event.device.id))
                    }
                }
            }
        }

        private suspend fun loadDevices(
            onSuccess: (List<Device>) -> Unit,
            onError: (String) -> Unit,
        ) {
            try {
                // Get API token from preferences
                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                val apiToken = preferences.apiToken

                if (apiToken.isNullOrBlank()) {
                    onError("API token not found. Please configure your token.")
                    return
                }

                // Call API with Bearer token
                val authHeader = "Bearer $apiToken"
                when (val result = apiService.getDevices(authHeader)) {
                    is ApiResult.Success -> {
                        onSuccess(result.value.data)
                    }

                    is ApiResult.Failure.HttpFailure -> {
                        when (result.code) {
                            401 -> onError("Unauthorized. Please check your API token.")
                            404 -> onError("API endpoint not found.")
                            else -> onError("HTTP Error: ${result.code}")
                        }
                    }

                    is ApiResult.Failure.NetworkFailure -> {
                        onError("Network error. Please check your connection.")
                    }

                    is ApiResult.Failure.ApiFailure -> {
                        onError("API Error: ${result.error}")
                    }

                    is ApiResult.Failure.UnknownFailure -> {
                        onError("Unknown error: ${result.error.message}")
                    }
                }
            } catch (e: Exception) {
                onError("Error loading devices: ${e.message}")
            }
        }

        @CircuitInject(TrmnlDevicesScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): TrmnlDevicesPresenter
        }
    }

/**
 * UI content for TrmnlDevicesScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(TrmnlDevicesScreen::class, AppScope::class)
@Composable
fun TrmnlDevicesContent(
    state: TrmnlDevicesScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("TRMNL Devices") },
                actions = {
                    IconButton(onClick = { state.eventSink(TrmnlDevicesScreen.Event.AccountClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.account_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Account",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Loading devices...")
                    }
                }
            }

            state.errorMessage != null -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.devices.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.nest_display_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "No devices",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "Your TRMNL devices will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                // Devices list
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.devices) { device ->
                        DeviceCard(
                            device = device,
                            onClick = { state.eventSink(TrmnlDevicesScreen.Event.DeviceClicked(device)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            supportingContent = {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Device ID
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "ID: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = device.friendlyId,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // MAC Address
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "MAC: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = device.macAddress,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Battery Level
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Battery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${device.percentCharged.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = getBatteryColor(device.percentCharged),
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (device.percentCharged / 100).toFloat() },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            color = getBatteryColor(device.percentCharged),
                        )
                        device.batteryVoltage?.let { voltage ->
                            Text(
                                text = "%.2fV".format(voltage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // WiFi Strength
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "WiFi Signal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${device.wifiStrength.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = getWifiColor(device.wifiStrength),
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (device.wifiStrength / 100).toFloat() },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            color = getWifiColor(device.wifiStrength),
                        )
                        device.rssi?.let { rssi ->
                            Text(
                                text = "$rssi dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.nest_display_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Device",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        )
    }
}

@Composable
private fun getBatteryColor(percentCharged: Double): Color {
    // Use theme-aware colors that work with both light and dark themes
    val goodColor = MaterialTheme.colorScheme.tertiary
    val warningColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val criticalColor = MaterialTheme.colorScheme.error

    return when {
        percentCharged >= 60 -> goodColor // Good battery level
        percentCharged >= 30 -> warningColor // Warning level
        else -> criticalColor // Critical level
    }
}

@Composable
private fun getWifiColor(wifiStrength: Double): Color {
    // Use theme-aware colors that work with both light and dark themes
    val strongColor = MaterialTheme.colorScheme.tertiary
    val mediumColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val weakColor = MaterialTheme.colorScheme.error

    return when {
        wifiStrength >= 70 -> strongColor // Strong signal
        wifiStrength >= 40 -> mediumColor // Medium signal
        else -> weakColor // Weak signal
    }
}
