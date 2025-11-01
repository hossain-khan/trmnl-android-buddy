package ink.trmnl.android.buddy.ui.devices

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
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
import ink.trmnl.android.buddy.content.models.ContentItem
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen
import ink.trmnl.android.buddy.ui.devicepreview.DevicePreviewScreen
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.util.BrowserUtils
import ink.trmnl.android.buddy.util.formatRefreshRateExplanation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * Data class to hold preview image information including refresh rate.
 */
data class DevicePreviewInfo(
    val imageUrl: String,
    val refreshRate: Int, // in seconds
)

/**
 * Screen for displaying list of TRMNL devices.
 * Shows device status, battery level, and WiFi strength.
 */
@Parcelize
data object TrmnlDevicesScreen : Screen {
    data class State(
        val devices: List<Device> = emptyList(),
        val deviceTokens: Map<String, String?> = emptyMap(),
        val devicePreviews: Map<String, DevicePreviewInfo?> = emptyMap(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val isUnauthorized: Boolean = false,
        val isPrivacyEnabled: Boolean = true,
        val snackbarMessage: String? = null,
        val latestContent: List<ContentItem> = emptyList(),
        val isContentLoading: Boolean = true,
        val isRssFeedContentEnabled: Boolean = true,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object Refresh : Event()

        data object SettingsClicked : Event()

        data object TogglePrivacy : Event()

        data object ResetToken : Event()

        data class DeviceClicked(
            val device: Device,
        ) : Event()

        data class DeviceSettingsClicked(
            val device: Device,
        ) : Event()

        data class DevicePreviewClicked(
            val device: Device,
            val previewInfo: DevicePreviewInfo,
        ) : Event()

        data class RefreshRateInfoClicked(
            val refreshRate: Int,
        ) : Event()

        data class ContentItemClicked(
            val item: ContentItem,
        ) : Event()

        data object ViewAllContentClicked : Event()

        data object DismissSnackbar : Event()
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
        @ApplicationContext private val context: Context,
        private val apiService: TrmnlApiService,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val deviceTokenRepository: DeviceTokenRepository,
        private val contentFeedRepository: ink.trmnl.android.buddy.content.repository.ContentFeedRepository,
        private val announcementRepository: ink.trmnl.android.buddy.content.repository.AnnouncementRepository,
        private val blogPostRepository: ink.trmnl.android.buddy.content.repository.BlogPostRepository,
    ) : Presenter<TrmnlDevicesScreen.State> {
        @Composable
        override fun present(): TrmnlDevicesScreen.State {
            var devices by rememberRetained { mutableStateOf<List<Device>>(emptyList()) }
            var deviceTokens by rememberRetained { mutableStateOf<Map<String, String?>>(emptyMap()) }
            var devicePreviews by rememberRetained { mutableStateOf<Map<String, DevicePreviewInfo?>>(emptyMap()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            var isUnauthorized by rememberRetained { mutableStateOf(false) }
            var isPrivacyEnabled by rememberRetained { mutableStateOf(true) }
            var snackbarMessage by rememberRetained { mutableStateOf<String?>(null) }
            var latestContent by rememberRetained {
                mutableStateOf<List<ContentItem>>(emptyList())
            }
            var isContentLoading by rememberRetained { mutableStateOf(true) }
            var isRssFeedContentEnabled by rememberRetained { mutableStateOf(true) }
            val coroutineScope = rememberCoroutineScope()

            // Capture theme colors for Custom Tabs
            val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
            val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

            // Collect RSS feed content enabled preference
            LaunchedEffect(Unit) {
                userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                    isRssFeedContentEnabled = preferences.isRssFeedContentEnabled
                }
            }

            // Collect latest unread content (announcements + blog posts) from combined feed
            // Using getLatestUnreadContent() ensures we only get unread items, which is what the carousel displays
            LaunchedEffect(Unit) {
                contentFeedRepository.getLatestUnreadContent(limit = 3).collect { content ->
                    latestContent = content
                    isContentLoading = false
                }
            }

            // Fetch content on initial load (if database is empty)
            LaunchedEffect(Unit) {
                // Check if we have content, if not, fetch both announcements and blog posts
                if (latestContent.isEmpty()) {
                    // Fetch announcements and blog posts in parallel
                    coroutineScope.launch {
                        val jobs =
                            listOf(
                                async {
                                    val announcementResult = announcementRepository.refreshAnnouncements()
                                    if (announcementResult.isFailure) {
                                        timber.log.Timber.d(
                                            "Failed to fetch initial announcements: %s",
                                            announcementResult.exceptionOrNull()?.message,
                                        )
                                    }
                                },
                                async {
                                    val blogPostResult = blogPostRepository.refreshBlogPosts()
                                    if (blogPostResult.isFailure) {
                                        timber.log.Timber.d(
                                            "Failed to fetch initial blog posts: %s",
                                            blogPostResult.exceptionOrNull()?.message,
                                        )
                                    }
                                },
                            )
                        jobs.awaitAll()
                    }
                }
            }

            // Fetch devices on initial load only (when devices list is empty)
            LaunchedEffect(Unit) {
                if (devices.isEmpty() && errorMessage == null) {
                    loadDevices(
                        onSuccess = { fetchedDevices ->
                            devices = fetchedDevices
                            isLoading = false
                        },
                        onError = { error, unauthorized ->
                            errorMessage = error
                            isUnauthorized = unauthorized
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
                isUnauthorized = isUnauthorized,
                isPrivacyEnabled = isPrivacyEnabled,
                snackbarMessage = snackbarMessage,
                latestContent = latestContent,
                isContentLoading = isContentLoading,
                isRssFeedContentEnabled = isRssFeedContentEnabled,
            ) { event ->
                when (event) {
                    TrmnlDevicesScreen.Event.Refresh -> {
                        isLoading = true
                        isContentLoading = true
                        errorMessage = null
                        isUnauthorized = false
                        coroutineScope.launch {
                            // Refresh content (announcements + blog posts) in background
                            launch {
                                val announcementResult = announcementRepository.refreshAnnouncements()
                                if (announcementResult.isFailure) {
                                    snackbarMessage =
                                        "Failed to refresh announcements: ${announcementResult.exceptionOrNull()?.message}"
                                }
                            }
                            launch {
                                val blogPostResult = blogPostRepository.refreshBlogPosts()
                                if (blogPostResult.isFailure) {
                                    snackbarMessage =
                                        "Failed to refresh blog posts: ${blogPostResult.exceptionOrNull()?.message}"
                                }
                            }

                            // Refresh devices
                            loadDevices(
                                onSuccess = { fetchedDevices ->
                                    devices = fetchedDevices
                                    // LaunchedEffects will handle loading tokens and previews
                                    isLoading = false
                                },
                                onError = { error, unauthorized ->
                                    errorMessage = error
                                    isUnauthorized = unauthorized
                                    isLoading = false
                                },
                            )
                        }
                    }

                    is TrmnlDevicesScreen.Event.SettingsClicked -> {
                        navigator.goTo(ink.trmnl.android.buddy.ui.settings.SettingsScreen)
                    }

                    TrmnlDevicesScreen.Event.TogglePrivacy -> {
                        isPrivacyEnabled = !isPrivacyEnabled
                        snackbarMessage =
                            if (isPrivacyEnabled) {
                                "Device ID and MAC address hidden for privacy"
                            } else {
                                "Device ID and MAC address now visible"
                            }
                    }

                    TrmnlDevicesScreen.Event.ResetToken -> {
                        coroutineScope.launch {
                            // Clear all token data from preferences
                            userPreferencesRepository.clearApiToken()
                            // Navigate to AccessTokenScreen with root reset
                            navigator.resetRoot(ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen)
                        }
                    }

                    is TrmnlDevicesScreen.Event.DeviceClicked -> {
                        navigator.goTo(
                            ink.trmnl.android.buddy.ui.devicedetail.DeviceDetailScreen(
                                deviceId = event.device.friendlyId,
                                deviceName = event.device.name,
                                currentBattery = event.device.percentCharged,
                                currentVoltage = event.device.batteryVoltage,
                                wifiStrength = event.device.wifiStrength,
                                rssi = event.device.rssi,
                            ),
                        )
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
                                imageUrl = event.previewInfo.imageUrl,
                            ),
                        )
                    }

                    is TrmnlDevicesScreen.Event.RefreshRateInfoClicked -> {
                        snackbarMessage = formatRefreshRateExplanation(event.refreshRate)
                    }

                    is TrmnlDevicesScreen.Event.ContentItemClicked -> {
                        // Open content in Chrome Custom Tabs
                        BrowserUtils.openUrlInCustomTab(
                            context = context,
                            url = event.item.link,
                            toolbarColor = primaryColor,
                            secondaryColor = surfaceColor,
                        )
                        // Mark content as read based on type
                        coroutineScope.launch {
                            when (event.item) {
                                is ContentItem.Announcement -> {
                                    announcementRepository.markAsRead(event.item.id)
                                }
                                is ContentItem.BlogPost -> {
                                    blogPostRepository.markAsRead(event.item.id)
                                }
                            }
                        }
                    }

                    TrmnlDevicesScreen.Event.ViewAllContentClicked -> {
                        navigator.goTo(ContentHubScreen)
                    }

                    TrmnlDevicesScreen.Event.DismissSnackbar -> {
                        snackbarMessage = null
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
            onLoaded: (Map<String, DevicePreviewInfo?>) -> Unit,
        ) {
            val previews =
                devices.associate { device ->
                    val token = tokens[device.friendlyId]
                    val previewInfo =
                        if (token != null) {
                            try {
                                when (val result = apiService.getDisplayCurrent(token)) {
                                    is ApiResult.Success -> {
                                        result.value.imageUrl?.let { imageUrl ->
                                            DevicePreviewInfo(
                                                imageUrl = imageUrl,
                                                refreshRate = result.value.refreshRate,
                                            )
                                        }
                                    }
                                    else -> null // Silently fail for preview images
                                }
                            } catch (e: Exception) {
                                Timber.d(e, "Error loading preview for %s", device.name)
                                null // Silently fail for preview images
                            }
                        } else {
                            null
                        }
                    device.friendlyId to previewInfo
                }
            onLoaded(previews)
        }

        private suspend fun loadDevices(
            onSuccess: (List<Device>) -> Unit,
            onError: (String, Boolean) -> Unit,
        ) {
            try {
                // Get API token from preferences
                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                val apiToken = preferences.apiToken

                if (apiToken.isNullOrBlank()) {
                    onError("API token not found. Please configure your token.", false)
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
                            401 -> onError("Unauthorized. Please check your API token.", true)
                            404 -> onError("API endpoint not found.", false)
                            else -> onError("HTTP Error: ${result.code}", false)
                        }
                    }

                    is ApiResult.Failure.NetworkFailure -> {
                        onError("Network error. Please check your connection.", false)
                    }

                    is ApiResult.Failure.ApiFailure -> {
                        onError("API Error: ${result.error}", false)
                    }

                    is ApiResult.Failure.UnknownFailure -> {
                        onError("Unknown error: ${result.error.message}", false)
                    }
                }
            } catch (e: Exception) {
                onError("Error loading devices: ${e.message}", false)
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when message changes
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            state.eventSink(TrmnlDevicesScreen.Event.DismissSnackbar)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("TRMNL Devices") },
                actions = {
                    IconButton(onClick = { state.eventSink(TrmnlDevicesScreen.Event.TogglePrivacy) }) {
                        Icon(
                            painter =
                                painterResource(
                                    if (state.isPrivacyEnabled) {
                                        R.drawable.password_2_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    } else {
                                        R.drawable.password_2_off_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    },
                                ),
                            contentDescription = if (state.isPrivacyEnabled) "Privacy enabled" else "Privacy disabled",
                        )
                    }
                    IconButton(onClick = { state.eventSink(TrmnlDevicesScreen.Event.SettingsClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingState()
            }

            state.errorMessage != null -> {
                ErrorState(
                    errorMessage = state.errorMessage,
                    isUnauthorized = state.isUnauthorized,
                    onResetToken = { state.eventSink(TrmnlDevicesScreen.Event.ResetToken) },
                )
            }

            state.devices.isEmpty() -> {
                EmptyState()
            }

            else -> {
                DevicesList(
                    devices = state.devices,
                    deviceTokens = state.deviceTokens,
                    devicePreviews = state.devicePreviews,
                    isPrivacyEnabled = state.isPrivacyEnabled,
                    innerPadding = innerPadding,
                    latestContent = state.latestContent,
                    isContentLoading = state.isContentLoading,
                    isRssFeedContentEnabled = state.isRssFeedContentEnabled,
                    onDeviceClick = { device -> state.eventSink(TrmnlDevicesScreen.Event.DeviceClicked(device)) },
                    onSettingsClick = { device -> state.eventSink(TrmnlDevicesScreen.Event.DeviceSettingsClicked(device)) },
                    onPreviewClick = { device, previewInfo ->
                        state.eventSink(
                            TrmnlDevicesScreen.Event.DevicePreviewClicked(
                                device = device,
                                previewInfo = previewInfo,
                            ),
                        )
                    },
                    onContentItemClick = { item ->
                        state.eventSink(TrmnlDevicesScreen.Event.ContentItemClicked(item))
                    },
                    onViewAllContentClick = {
                        state.eventSink(TrmnlDevicesScreen.Event.ViewAllContentClicked)
                    },
                    eventSink = state.eventSink,
                )
            }
        }
    }
}

// ========== Previews ==========

/**
 * Sample device data for previews.
 */
private val sampleDevice1 =
    Device(
        id = 1,
        name = "Living Room Display",
        friendlyId = "ABC-123",
        macAddress = "12:34:56:78:9A:BC",
        batteryVoltage = 3.7,
        rssi = -45,
        percentCharged = 85.0,
        wifiStrength = 90.0,
    )

private val sampleDevice2 =
    Device(
        id = 2,
        name = "Kitchen Dashboard",
        friendlyId = "DEF-456",
        macAddress = "12:34:56:78:9A:BD",
        batteryVoltage = 3.5,
        rssi = -65,
        percentCharged = 45.0,
        wifiStrength = 60.0,
    )

private val sampleDevice3 =
    Device(
        id = 3,
        name = "Office Monitor",
        friendlyId = "GHI-789",
        macAddress = "12:34:56:78:9A:BE",
        batteryVoltage = 3.2,
        rssi = -80,
        percentCharged = 15.0,
        wifiStrength = 25.0,
    )

@Preview(name = "Full Screen - With Devices", showBackground = true)
@Composable
private fun TrmnlDevicesContentPreview() {
    TrmnlBuddyAppTheme {
        TrmnlDevicesContent(
            state =
                TrmnlDevicesScreen.State(
                    devices = listOf(sampleDevice1, sampleDevice2, sampleDevice3),
                    deviceTokens = mapOf("ABC-123" to "token1", "DEF-456" to "token2"),
                    devicePreviews = emptyMap(),
                    isLoading = false,
                    errorMessage = null,
                    isUnauthorized = false,
                    isPrivacyEnabled = false,
                ),
        )
    }
}

@Preview(name = "Full Screen - Loading")
@Composable
private fun TrmnlDevicesContentLoadingPreview() {
    TrmnlBuddyAppTheme {
        TrmnlDevicesContent(
            state =
                TrmnlDevicesScreen.State(
                    devices = emptyList(),
                    isLoading = true,
                    errorMessage = null,
                ),
        )
    }
}

@Preview(name = "Full Screen - Error")
@Composable
private fun TrmnlDevicesContentErrorPreview() {
    TrmnlBuddyAppTheme {
        TrmnlDevicesContent(
            state =
                TrmnlDevicesScreen.State(
                    devices = emptyList(),
                    isLoading = false,
                    errorMessage = "Network error. Please check your connection.",
                    isUnauthorized = false,
                ),
        )
    }
}

@Preview(name = "Full Screen - Empty")
@Composable
private fun TrmnlDevicesContentEmptyPreview() {
    TrmnlBuddyAppTheme {
        TrmnlDevicesContent(
            state =
                TrmnlDevicesScreen.State(
                    devices = emptyList(),
                    isLoading = false,
                    errorMessage = null,
                ),
        )
    }
}
