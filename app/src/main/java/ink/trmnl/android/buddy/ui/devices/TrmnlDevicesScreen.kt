package ink.trmnl.android.buddy.ui.devices

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import ink.trmnl.android.buddy.content.models.ContentItem
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen
import ink.trmnl.android.buddy.ui.devicepreview.DevicePreviewScreen
import ink.trmnl.android.buddy.ui.sharedelements.DevicePreviewImageKey
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.ui.utils.getBatteryColor
import ink.trmnl.android.buddy.ui.utils.getBatteryIcon
import ink.trmnl.android.buddy.ui.utils.getWifiColor
import ink.trmnl.android.buddy.ui.utils.getWifiIcon
import ink.trmnl.android.buddy.util.BrowserUtils
import ink.trmnl.android.buddy.util.PrivacyUtils
import ink.trmnl.android.buddy.util.formatRefreshRate
import ink.trmnl.android.buddy.util.formatRefreshRateExplanation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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

            // Collect latest content (announcements + blog posts) from combined feed
            LaunchedEffect(Unit) {
                contentFeedRepository.getLatestContent(limit = 3).collect { content ->
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

/**
 * Loading state composable.
 * Shows a centered loading indicator with message.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
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

/**
 * Error state composable.
 * Shows error message and optional reset button.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    isUnauthorized: Boolean,
    onResetToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
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
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isUnauthorized) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onResetToken) {
                    Text("Reset Token")
                }
            }
        }
    }
}

/**
 * Empty state composable.
 * Shows when no devices are found.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
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

/**
 * Devices list composable.
 * Shows scrollable list of device cards with content carousel at top.
 */
@Composable
private fun DevicesList(
    devices: List<Device>,
    deviceTokens: Map<String, String?>,
    devicePreviews: Map<String, DevicePreviewInfo?>,
    isPrivacyEnabled: Boolean,
    innerPadding: PaddingValues,
    latestContent: List<ContentItem>,
    isContentLoading: Boolean,
    isRssFeedContentEnabled: Boolean,
    onDeviceClick: (Device) -> Unit,
    onSettingsClick: (Device) -> Unit,
    onPreviewClick: (Device, DevicePreviewInfo) -> Unit,
    onContentItemClick: (ContentItem) -> Unit,
    onViewAllContentClick: () -> Unit,
    eventSink: (TrmnlDevicesScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Content carousel at the top
        // Only show if RSS feed content is enabled AND there is unread content
        // Business Logic: Filter content to only include unread items (isRead = false)
        // and only display the carousel when there's at least one unread item
        val unreadContent = latestContent.filter { !it.isRead }
        if (isRssFeedContentEnabled && unreadContent.isNotEmpty()) {
            item {
                ContentCarousel(
                    content = unreadContent,
                    isLoading = isContentLoading,
                    onContentClick = onContentItemClick,
                    onViewAllClick = onViewAllContentClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(
            items = devices,
            key = { device -> device.id },
        ) { device ->
            DeviceCard(
                device = device,
                hasToken = deviceTokens[device.friendlyId] != null,
                previewInfo = devicePreviews[device.friendlyId],
                isPrivacyEnabled = isPrivacyEnabled,
                onClick = { onDeviceClick(device) },
                onSettingsClick = { onSettingsClick(device) },
                onPreviewClick = {
                    devicePreviews[device.friendlyId]?.let { previewInfo ->
                        onPreviewClick(device, previewInfo)
                    }
                },
                eventSink = eventSink,
            )
        }
    }
}

/**
 * Content carousel composable.
 * Shows combined feed of announcements and blog posts with post type indicators.
 * Features auto-rotation every 5 seconds that stops permanently once user manually swipes.
 * Includes accessibility improvements and lifecycle awareness.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentCarousel(
    content: List<ContentItem>,
    isLoading: Boolean,
    onContentClick: (ContentItem) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var isAppInForeground by remember { mutableStateOf(true) }
    var userIsInteracting by remember { mutableStateOf(false) }
    // Track if user has manually swiped - once true, auto-rotation stops permanently
    var hasUserManuallyPaged by remember { mutableStateOf(false) }

    // Observe lifecycle to pause auto-rotation when app is backgrounded
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                isAppInForeground = event == Lifecycle.Event.ON_RESUME
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Content carousel showing announcements and blog posts"
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with "View All" button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Announcements & Blog Posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onViewAllClick()
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "View all announcements and blog posts"
                        },
                ) {
                    Text(text = "View All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = null, // Decorative - text button already has description
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            when {
                isLoading -> {
                    // Loading skeleton with shimmer effect
                    LoadingSkeletonCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                content.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No content available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    // Display content carousel with auto-rotation
                    val pagerState = rememberPagerState(pageCount = { content.size })

                    // Detect when user manually swipes (drags) the pager
                    // This stops auto-rotation permanently once user demonstrates awareness of paging
                    // Uses interactionSource to distinguish user drags from programmatic animateScrollToPage()
                    LaunchedEffect(pagerState.interactionSource) {
                        snapshotFlow {
                            // True only when user is actively dragging the pager (not programmatic scroll)
                            pagerState.currentPageOffsetFraction != 0f &&
                                pagerState.isScrollInProgress
                        }.collect { isUserDragging ->
                            if (isUserDragging && !hasUserManuallyPaged) {
                                // User has manually dragged the pager - permanently disable auto-rotation
                                hasUserManuallyPaged = true
                            }
                        }
                    }

                    // Auto-rotation every 5 seconds
                    // Stops permanently once user manually swipes (hasUserManuallyPaged = true)
                    // Also pauses when app is backgrounded or user is pressing on content
                    LaunchedEffect(pagerState, content.size, isAppInForeground, userIsInteracting, hasUserManuallyPaged) {
                        // Only auto-rotate if user hasn't manually paged yet
                        while (isAppInForeground && !userIsInteracting && !hasUserManuallyPaged) {
                            delay(5000) // 5 seconds
                            val nextPage = (pagerState.currentPage + 1) % content.size
                            pagerState.animateScrollToPage(
                                page = nextPage,
                                animationSpec = tween(durationMillis = 500),
                            )
                        }
                    }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            userIsInteracting = true
                                            tryAwaitRelease()
                                            // Keep paused for a bit after release
                                            delay(2000)
                                            userIsInteracting = false
                                        },
                                    )
                                },
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            pageSpacing = 12.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) { page ->
                            val item = content[page]
                            ContentItemCard(
                                item = item,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onContentClick(item)
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .animatePageScale(pagerState, page),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Page indicators
                        PageIndicators(
                            pageCount = content.size,
                            currentPage = pagerState.currentPage,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .semantics {
                                        contentDescription = "Page ${pagerState.currentPage + 1} of ${content.size}"
                                    },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content item card composable.
 * Shows a single content item (announcement or blog post) with type indicator.
 * Includes accessibility improvements, semantic colors, proper touch targets, and animations.
 */
@Composable
private fun ContentItemCard(
    item: ContentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate card elevation on press
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardElevation",
    )

    // Animate card color on press
    val containerColor by animateColorAsState(
        targetValue =
            if (isPressed) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        animationSpec = tween(durationMillis = 150),
        label = "cardColor",
    )

    Card(
        modifier =
            modifier
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null, // Custom ripple via state layers
                ).semantics {
                    contentDescription =
                        buildString {
                            append(
                                when (item) {
                                    is ContentItem.Announcement -> "Announcement: "
                                    is ContentItem.BlogPost -> "Blog post: "
                                },
                            )
                            append(item.title)
                            if (!item.isRead) append(", unread")
                            append(", published ${formatRelativeDate(item.publishedDate)}")
                        }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title, post type chip, and unread badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Post type chip with proper semantics
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color =
                            when (item) {
                                is ContentItem.Announcement -> MaterialTheme.colorScheme.primaryContainer
                                is ContentItem.BlogPost -> MaterialTheme.colorScheme.secondaryContainer
                            },
                        modifier =
                            Modifier.semantics {
                                contentDescription =
                                    when (item) {
                                        is ContentItem.Announcement -> "Type: Announcement"
                                        is ContentItem.BlogPost -> "Type: Blog post"
                                    }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        when (item) {
                                            is ContentItem.Announcement ->
                                                R.drawable
                                                    .campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                            is ContentItem.BlogPost ->
                                                R.drawable
                                                    .newspaper_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                        },
                                    ),
                                contentDescription = null, // Decorative
                                modifier = Modifier.size(14.dp),
                                tint =
                                    when (item) {
                                        is ContentItem.Announcement -> MaterialTheme.colorScheme.onPrimaryContainer
                                        is ContentItem.BlogPost -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                            )
                            Text(
                                text =
                                    when (item) {
                                        is ContentItem.Announcement -> "Announcement"
                                        is ContentItem.BlogPost -> "Blog"
                                    },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color =
                                    when (item) {
                                        is ContentItem.Announcement -> MaterialTheme.colorScheme.onPrimaryContainer
                                        is ContentItem.BlogPost -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                            )
                        }
                    }

                    // Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }

                // Unread badge with minimum touch target
                if (!item.isRead) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .size(12.dp)
                                .semantics {
                                    contentDescription = "Unread"
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        ) {}
                    }
                }
            }

            // Summary
            if (item.summary.isNotEmpty()) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatRelativeDate(item.publishedDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Blog-specific metadata
                if (item is ContentItem.BlogPost) {
                    item.category?.let { category ->
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Page indicators showing current position in carousel.
 * Uses semantic colors and proper sizing for accessibility.
 */
@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val size by animateFloatAsState(
                targetValue = if (isSelected) 10f else 8f,
                animationSpec = tween(durationMillis = 200),
                label = "indicatorSize",
            )
            Surface(
                shape = CircleShape,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                modifier =
                    Modifier
                        .size(size.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Page ${index + 1}${if (isSelected) " selected" else ""}"
                        },
            ) {}
        }
    }
}

/**
 * Animate page scale for subtle zoom effect during transitions.
 * Pages that are not current are slightly scaled down.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.animatePageScale(
    pagerState: PagerState,
    page: Int,
): Modifier {
    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val scale by animateFloatAsState(
        targetValue = if (kotlin.math.abs(pageOffset) < 0.5f) 1f else 0.92f,
        animationSpec = tween(durationMillis = 300),
        label = "pageScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (kotlin.math.abs(pageOffset) < 0.5f) 1f else 0.7f,
        animationSpec = tween(durationMillis = 300),
        label = "pageAlpha",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
}

/**
 * Loading skeleton with shimmer effect.
 * Provides visual feedback while content is loading.
 */
@Composable
private fun LoadingSkeletonCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmerAlpha",
    )

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Type chip skeleton
            Box(
                modifier =
                    Modifier
                        .width(100.dp)
                        .height(24.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha)),
            )

            // Title skeleton
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(24.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha)),
            )

            // Summary skeleton
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha),
                            ),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha),
                            ),
                )
            }

            // Metadata skeleton
            Box(
                modifier =
                    Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha)),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DeviceCard(
    device: Device,
    hasToken: Boolean,
    previewInfo: DevicePreviewInfo?,
    isPrivacyEnabled: Boolean,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPreviewClick: () -> Unit,
    eventSink: (TrmnlDevicesScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                                        R.drawable.tv_options_input_settings_24dp_e8eaed_fill0_wght400_grad0_opsz24
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
                    DeviceInfoRow(
                        icon = R.drawable.outline_tag_24,
                        label = "ID: ",
                        value =
                            if (isPrivacyEnabled) {
                                PrivacyUtils.obfuscateDeviceId(device.friendlyId)
                            } else {
                                device.friendlyId
                            },
                        contentDescription = "Device ID",
                    )

                    // MAC Address (obfuscated for privacy)
                    DeviceInfoRow(
                        icon = R.drawable.outline_barcode_24,
                        label = "MAC: ",
                        value =
                            if (isPrivacyEnabled) {
                                PrivacyUtils.obfuscateMacAddress(device.macAddress)
                            } else {
                                device.macAddress
                            },
                        contentDescription = "MAC Address",
                    )

                    // Battery Level
                    BatteryIndicator(
                        percentCharged = device.percentCharged,
                        batteryVoltage = device.batteryVoltage,
                        batteryProgress = batteryProgress,
                    )

                    // WiFi Strength
                    WifiIndicator(
                        wifiStrength = device.wifiStrength,
                        rssi = device.rssi,
                        wifiProgress = wifiProgress,
                    )
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
        DevicePreviewImage(
            hasToken = hasToken,
            previewInfo = previewInfo,
            deviceName = device.name,
            deviceId = device.friendlyId,
            onPreviewClick = onPreviewClick,
            eventSink = eventSink,
        )
    }
}

/**
 * Device information row composable.
 * Shows an icon, label, and value in a horizontal row.
 */
@Composable
private fun DeviceInfoRow(
    icon: Int,
    label: String,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Battery indicator composable.
 * Shows battery percentage, progress bar, and optional voltage.
 */
@Composable
private fun BatteryIndicator(
    percentCharged: Double,
    batteryVoltage: Double?,
    batteryProgress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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
                    painter = painterResource(getBatteryIcon(percentCharged)),
                    contentDescription = "Battery",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Battery",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${percentCharged.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = getBatteryColor(percentCharged),
            )
        }
        LinearProgressIndicator(
            progress = { batteryProgress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            color = getBatteryColor(percentCharged),
        )
        batteryVoltage?.let { voltage ->
            Text(
                text = "%.2fV".format(voltage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * WiFi indicator composable.
 * Shows WiFi signal strength percentage, progress bar, and optional RSSI.
 */
@Composable
private fun WifiIndicator(
    wifiStrength: Double,
    rssi: Int?,
    wifiProgress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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
                    painter = painterResource(getWifiIcon(wifiStrength)),
                    contentDescription = "WiFi Signal",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "WiFi Signal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${wifiStrength.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = getWifiColor(wifiStrength),
            )
        }
        LinearProgressIndicator(
            progress = { wifiProgress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            color = getWifiColor(wifiStrength),
        )
        rssi?.let { rssiValue ->
            Text(
                text = "$rssiValue dBm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Device preview image composable.
 * Shows the device's current screen display if available.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DevicePreviewImage(
    hasToken: Boolean,
    previewInfo: DevicePreviewInfo?,
    deviceName: String,
    deviceId: String,
    onPreviewClick: () -> Unit,
    eventSink: (TrmnlDevicesScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasToken && previewInfo != null) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(),
        ) {
            SharedElementTransitionScope {
                Box(
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .aspectRatio(800f / 480f) // TRMNL device aspect ratio
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    SubcomposeAsyncImage(
                        model = previewInfo.imageUrl,
                        contentDescription = "Device screen preview for $deviceName",
                        modifier =
                            Modifier
                                .sharedElement(
                                    sharedContentState =
                                        rememberSharedContentState(
                                            key = DevicePreviewImageKey(deviceId = deviceId),
                                        ),
                                    animatedVisibilityScope = requireAnimatedScope(Navigation),
                                ).fillMaxSize()
                                .clickable(onClick = onPreviewClick),
                        contentScale = ContentScale.Fit,
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

                    // Refresh rate indicator overlay
                    RefreshRateIndicator(
                        refreshRate = previewInfo.refreshRate,
                        onInfoClick = { rate ->
                            eventSink(TrmnlDevicesScreen.Event.RefreshRateInfoClicked(rate))
                        },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

/**
 * Refresh rate indicator composable.
 * Shows a semi-transparent overlay with refresh rate information.
 */
@Composable
private fun RefreshRateIndicator(
    refreshRate: Int,
    onInfoClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onInfoClick(refreshRate) },
        modifier =
            modifier
                .padding(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            ),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = "Refresh rate",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatRefreshRate(refreshRate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ========== Helper Functions ==========

/**
 * Format Instant as relative time string (e.g., "2 days ago").
 */
private fun formatRelativeDate(instant: java.time.Instant): String {
    val now = java.time.Instant.now()
    val days =
        java.time.temporal.ChronoUnit.DAYS
            .between(instant, now)
    val hours =
        java.time.temporal.ChronoUnit.HOURS
            .between(instant, now)
    val minutes =
        java.time.temporal.ChronoUnit.MINUTES
            .between(instant, now)

    return when {
        days > 0 -> "$days day${if (days == 1L) "" else "s"} ago"
        hours > 0 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        minutes > 0 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        else -> "Just now"
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

/**
 * Sample content items for previews.
 */
private val sampleAnnouncement =
    ContentItem.Announcement(
        id = "1",
        title = "New Feature: Screen Sharing",
        summary =
            "We've added the ability to share your screen content with others. " +
                "Check out the new settings panel to get started.",
        link = "https://usetrmnl.com/announcements/screen-sharing",
        publishedDate =
            java.time.Instant
                .now()
                .minus(2, java.time.temporal.ChronoUnit.DAYS),
        isRead = false,
    )

private val sampleBlogPost =
    ContentItem.BlogPost(
        id = "2",
        title = "Building the Perfect E-Ink Dashboard",
        summary =
            "Learn how to create an efficient and beautiful dashboard for your TRMNL device. " +
                "We'll cover layout design, data sources, and optimization tips.",
        link = "https://usetrmnl.com/blog/perfect-dashboard",
        publishedDate =
            java.time.Instant
                .now()
                .minus(5, java.time.temporal.ChronoUnit.HOURS),
        isRead = false,
        authorName = "John Doe",
        category = "Tutorial",
        featuredImageUrl = "https://usetrmnl.com/images/blog/dashboard.jpg",
        isFavorite = false,
    )

private val sampleContentList =
    listOf(
        sampleBlogPost, // Recent blog post (5 hours ago)
        sampleAnnouncement, // Older announcement (2 days ago)
    )

@Preview(name = "Loading State")
@Composable
private fun LoadingStatePreview() {
    TrmnlBuddyAppTheme {
        LoadingState()
    }
}

@Preview(name = "Error State - Unauthorized")
@Composable
private fun ErrorStateUnauthorizedPreview() {
    TrmnlBuddyAppTheme {
        ErrorState(
            errorMessage = "Unauthorized. Please check your API token.",
            isUnauthorized = true,
            onResetToken = {},
        )
    }
}

@Preview(name = "Error State - Network Error")
@Composable
private fun ErrorStateNetworkPreview() {
    TrmnlBuddyAppTheme {
        ErrorState(
            errorMessage = "Network error. Please check your connection.",
            isUnauthorized = false,
            onResetToken = {},
        )
    }
}

@Preview(name = "Empty State")
@Composable
private fun EmptyStatePreview() {
    TrmnlBuddyAppTheme {
        EmptyState()
    }
}

@PreviewLightDark
@Preview(name = "Device Card - High Battery & WiFi")
@Composable
private fun DeviceCardHighBatteryPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice1,
            hasToken = true,
            previewInfo = null,
            isPrivacyEnabled = false,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Preview(name = "Device Card - Medium Battery & WiFi")
@Composable
private fun DeviceCardMediumBatteryPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice2,
            hasToken = true,
            previewInfo = null,
            isPrivacyEnabled = false,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Preview(name = "Device Card - Low Battery & WiFi")
@Composable
private fun DeviceCardLowBatteryPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice3,
            hasToken = false,
            previewInfo = null,
            isPrivacyEnabled = false,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Preview(name = "Device Card - Privacy Enabled")
@Composable
private fun DeviceCardPrivacyEnabledPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice1,
            hasToken = true,
            previewInfo = null,
            isPrivacyEnabled = true,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Preview(name = "Devices List with Multiple Devices")
@Composable
private fun DevicesListPreview() {
    TrmnlBuddyAppTheme {
        DevicesList(
            devices = listOf(sampleDevice1, sampleDevice2, sampleDevice3),
            deviceTokens = mapOf("ABC-123" to "token1", "DEF-456" to "token2"),
            devicePreviews = emptyMap(),
            isPrivacyEnabled = false,
            innerPadding = PaddingValues(0.dp),
            latestContent = sampleContentList,
            isContentLoading = false,
            isRssFeedContentEnabled = true,
            onDeviceClick = {},
            onSettingsClick = {},
            onPreviewClick = { _, _ -> },
            onContentItemClick = {},
            onViewAllContentClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
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
