package ink.trmnl.android.buddy.ui.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.devicepreview.DevicePreviewScreen
import ink.trmnl.android.buddy.ui.sharedelements.DevicePreviewImageKey
import ink.trmnl.android.buddy.ui.utils.rememberEInkColorFilter
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
        val deviceTokens: Map<String, String?> = emptyMap(),
        val devicePreviews: Map<String, String?> = emptyMap(),
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

        data class DeviceSettingsClicked(
            val device: Device,
        ) : Event()

        data class DevicePreviewClicked(
            val device: Device,
            val imageUrl: String,
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
        private val deviceTokenRepository: DeviceTokenRepository,
    ) : Presenter<TrmnlDevicesScreen.State> {
        @Composable
        override fun present(): TrmnlDevicesScreen.State {
            var devices by rememberRetained { mutableStateOf<List<Device>>(emptyList()) }
            var deviceTokens by rememberRetained { mutableStateOf<Map<String, String?>>(emptyMap()) }
            var devicePreviews by rememberRetained { mutableStateOf<Map<String, String?>>(emptyMap()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // Fetch devices on initial load only (when devices list is empty)
            LaunchedEffect(Unit) {
                if (devices.isEmpty() && errorMessage == null) {
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

            // Always reload device tokens on every visit
            LaunchedEffect(devices) {
                if (devices.isNotEmpty()) {
                    loadDeviceTokens(devices) { tokens ->
                        val oldTokens = deviceTokens
                        deviceTokens = tokens

                        // Only reload previews if tokens actually changed or if we have new devices with tokens
                        val tokensChanged = oldTokens != tokens
                        val hasNewTokens =
                            tokens.values.any { it != null } &&
                                tokens.keys.any { deviceId ->
                                    oldTokens[deviceId] != tokens[deviceId] && tokens[deviceId] != null
                                }

                        if (tokensChanged || hasNewTokens) {
                            coroutineScope.launch {
                                loadDevicePreviews(devices, tokens) { previews ->
                                    devicePreviews = previews
                                }
                            }
                        }
                    }
                }
            }

            return TrmnlDevicesScreen.State(
                devices = devices,
                deviceTokens = deviceTokens,
                devicePreviews = devicePreviews,
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
                                    // LaunchedEffects will handle loading tokens and previews
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

                    is TrmnlDevicesScreen.Event.DeviceSettingsClicked -> {
                        navigator.goTo(
                            ink.trmnl.android.buddy.ui.devicetoken.DeviceTokenScreen(
                                deviceFriendlyId = event.device.friendlyId,
                                deviceName = event.device.name,
                            ),
                        )
                    }

                    is TrmnlDevicesScreen.Event.DevicePreviewClicked -> {
                        navigator.goTo(
                            DevicePreviewScreen(
                                deviceId = event.device.friendlyId,
                                deviceName = event.device.name,
                                imageUrl = event.imageUrl,
                            ),
                        )
                    }
                }
            }
        }

        private suspend fun loadDeviceTokens(
            devices: List<Device>,
            onLoaded: (Map<String, String?>) -> Unit,
        ) {
            val tokens =
                devices.associate { device ->
                    device.friendlyId to deviceTokenRepository.getDeviceToken(device.friendlyId)
                }
            onLoaded(tokens)
        }

        private suspend fun loadDevicePreviews(
            devices: List<Device>,
            tokens: Map<String, String?>,
            onLoaded: (Map<String, String?>) -> Unit,
        ) {
            val previews =
                devices.associate { device ->
                    val token = tokens[device.friendlyId]
                    val imageUrl =
                        if (token != null) {
                            try {
                                when (val result = apiService.getDisplayCurrent(token)) {
                                    is ApiResult.Success -> result.value.imageUrl
                                    else -> null // Silently fail for preview images
                                }
                            } catch (e: Exception) {
                                null // Silently fail for preview images
                            }
                        } else {
                            null
                        }
                    device.friendlyId to imageUrl
                }
            onLoaded(previews)
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
                            painter = painterResource(R.drawable.trmnl_device_frame),
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
                            hasToken = state.deviceTokens[device.friendlyId] != null,
                            previewImageUrl = state.devicePreviews[device.friendlyId],
                            onClick = { state.eventSink(TrmnlDevicesScreen.Event.DeviceClicked(device)) },
                            onSettingsClick = { state.eventSink(TrmnlDevicesScreen.Event.DeviceSettingsClicked(device)) },
                            onPreviewClick = {
                                state.devicePreviews[device.friendlyId]?.let { imageUrl ->
                                    state.eventSink(
                                        TrmnlDevicesScreen.Event.DevicePreviewClicked(
                                            device = device,
                                            imageUrl = imageUrl,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DeviceCard(
    device: Device,
    hasToken: Boolean,
    previewImageUrl: String?,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Invert colors in dark mode for better visibility of e-ink display images
    val colorFilter = rememberEInkColorFilter()

    // Track if this is the first composition to trigger animation
    var isInitialized by remember { mutableStateOf(false) }

    // Animate progress indicators from 0 to actual value on first load
    val batteryProgress by animateFloatAsState(
        targetValue = if (isInitialized) (device.percentCharged / 100).toFloat() else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 100),
        label = "battery_progress",
    )
    val wifiProgress by animateFloatAsState(
        targetValue = if (isInitialized) (device.wifiStrength / 100).toFloat() else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200),
        label = "wifi_progress",
    )

    // Trigger animation on first composition
    LaunchedEffect(Unit) {
        isInitialized = true
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (hasToken) {
                                        R.drawable.settings_check_24dp_e8eaed_fill1_wght400_grad0_opsz24
                                    } else {
                                        R.drawable.settings_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    },
                                ),
                            contentDescription = if (hasToken) "Display configured" else "Configure device token",
                            tint =
                                if (hasToken) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            },
            supportingContent = {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Device ID (obfuscated for privacy)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_tag_24),
                            contentDescription = "Device ID",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "ID: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = obfuscateDeviceId(device.friendlyId),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // MAC Address (obfuscated for privacy)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_barcode_24),
                            contentDescription = "MAC Address",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "MAC: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = obfuscateMacAddress(device.macAddress),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(getBatteryIcon(device.percentCharged)),
                                    contentDescription = "Battery",
                                    modifier = Modifier.size(16.dp),
                                    tint = getBatteryColor(device.percentCharged),
                                )
                                Text(
                                    text = "Battery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${device.percentCharged.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = getBatteryColor(device.percentCharged),
                            )
                        }
                        LinearProgressIndicator(
                            progress = { batteryProgress },
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(getWifiIcon(device.wifiStrength)),
                                    contentDescription = "WiFi Signal",
                                    modifier = Modifier.size(16.dp),
                                    tint = getWifiColor(device.wifiStrength),
                                )
                                Text(
                                    text = "WiFi Signal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${device.wifiStrength.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = getWifiColor(device.wifiStrength),
                            )
                        }
                        LinearProgressIndicator(
                            progress = { wifiProgress },
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
                    painter = painterResource(R.drawable.trmnl_device_frame),
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

        // Display preview image if available
        if (hasToken && previewImageUrl != null) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
            ) {
                SharedElementTransitionScope {
                    SubcomposeAsyncImage(
                        model = previewImageUrl,
                        contentDescription = "Device screen preview for ${device.name}",
                        modifier =
                            Modifier
                                .sharedElement(
                                    sharedContentState =
                                        rememberSharedContentState(
                                            key = DevicePreviewImageKey(deviceId = device.friendlyId),
                                        ),
                                    animatedVisibilityScope = requireAnimatedScope(Navigation),
                                ).fillMaxWidth()
                                .aspectRatio(800f / 480f) // TRMNL device aspect ratio
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable(onClick = onPreviewClick),
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        error = {
                            // Silently fail - don't show error for preview images
                        },
                    )
                }
            }
        }
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

/**
 * Obfuscates a MAC address for privacy by showing only the first and last segments.
 * Example: "AB:CD:EF:12:34:56" becomes "AB:••:••:••:••:56"
 */
private fun obfuscateMacAddress(macAddress: String): String {
    if (macAddress.length < 4) return macAddress

    val parts = macAddress.split(":")
    if (parts.size <= 2) {
        // If it's not a standard MAC address format, just obfuscate the middle
        return "${macAddress.take(2)}${"•".repeat(macAddress.length - 4)}${macAddress.takeLast(2)}"
    }

    // Standard MAC address format (e.g., "AB:CD:EF:12:34:56")
    // Show first part and last part, obfuscate everything in between with centered bullets
    return "${parts.first()}:${"••:".repeat(parts.size - 2)}${parts.last()}"
}

/**
 * Obfuscates a device ID for privacy by showing only the first character and last 2 characters.
 * Example: "ABC123" becomes "A•••23"
 */
private fun obfuscateDeviceId(deviceId: String): String {
    if (deviceId.length <= 3) return deviceId

    val first = deviceId.take(1)
    val last = deviceId.takeLast(2)
    val middle = "•".repeat(deviceId.length - 3)

    return "$first$middle$last"
}

/**
 * Returns the appropriate battery icon based on the charge percentage.
 */
private fun getBatteryIcon(percentCharged: Double): Int =
    when {
        percentCharged >= 95 -> R.drawable.outline_battery_android_full_24
        percentCharged >= 80 -> R.drawable.outline_battery_android_6_24
        percentCharged >= 65 -> R.drawable.outline_battery_android_5_24
        percentCharged >= 50 -> R.drawable.outline_battery_android_4_24
        percentCharged >= 35 -> R.drawable.outline_battery_android_3_24
        percentCharged >= 20 -> R.drawable.outline_battery_android_2_24
        percentCharged >= 5 -> R.drawable.outline_battery_android_1_24
        else -> R.drawable.outline_battery_android_0_24
    }

/**
 * Returns the appropriate WiFi signal icon based on signal strength percentage.
 */
private fun getWifiIcon(wifiStrength: Double): Int =
    when {
        wifiStrength >= 75 -> R.drawable.outline_signal_wifi_4_bar_24
        wifiStrength >= 50 -> R.drawable.outline_network_wifi_3_bar_24
        wifiStrength >= 25 -> R.drawable.outline_network_wifi_2_bar_24
        else -> R.drawable.outline_network_wifi_1_bar_24
    }
