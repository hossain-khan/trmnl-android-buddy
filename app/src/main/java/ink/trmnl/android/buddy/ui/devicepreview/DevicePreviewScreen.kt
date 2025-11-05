package ink.trmnl.android.buddy.ui.devicepreview

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
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
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.sharedelements.DevicePreviewImageKey
import ink.trmnl.android.buddy.util.ImageDownloadUtils
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

/**
 * Screen for displaying device preview image in full-screen.
 * Uses shared element transitions for smooth animation from the list.
 */
@Parcelize
data class DevicePreviewScreen(
    val deviceId: String,
    val deviceName: String,
    val imageUrl: String,
) : Screen {
    /**
     * Result returned when the preview screen is popped.
     * Contains the device ID and new image URL if the preview was refreshed.
     */
    @Parcelize
    data class Result(
        val deviceId: String,
        val newImageUrl: String?,
    ) : PopResult

    data class State(
        val deviceId: String,
        val deviceName: String,
        val imageUrl: String,
        val downloadState: DownloadState = DownloadState.Idle,
        val refreshState: RefreshState = RefreshState.Idle,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class DownloadState {
        data object Idle : DownloadState()

        data object Downloading : DownloadState()

        data class Success(
            val message: String,
        ) : DownloadState()

        data class Error(
            val message: String,
        ) : DownloadState()
    }

    sealed class RefreshState {
        data object Idle : RefreshState()

        data object Refreshing : RefreshState()

        data class Success(
            val newImageUrl: String,
            val message: String,
        ) : RefreshState()

        data class Error(
            val message: String,
        ) : RefreshState()
    }

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object DownloadImageClicked : Event()

        data object RefreshImageClicked : Event()

        data object DismissSnackbar : Event()

        data object DismissRefreshSnackbar : Event()
    }
}

/**
 * Presenter for DevicePreviewScreen.
 */
@Inject
class DevicePreviewPresenter
    constructor(
        @Assisted private val screen: DevicePreviewScreen,
        @Assisted private val navigator: Navigator,
        private val apiService: TrmnlApiService,
        private val deviceTokenRepository: DeviceTokenRepository,
    ) : Presenter<DevicePreviewScreen.State> {
        @Composable
        override fun present(): DevicePreviewScreen.State {
            var downloadState by rememberRetained {
                mutableStateOf<DevicePreviewScreen.DownloadState>(
                    DevicePreviewScreen.DownloadState.Idle,
                )
            }

            var refreshState by rememberRetained {
                mutableStateOf<DevicePreviewScreen.RefreshState>(
                    DevicePreviewScreen.RefreshState.Idle,
                )
            }

            var currentImageUrl by rememberRetained { mutableStateOf(screen.imageUrl) }

            val scope = rememberCoroutineScope()

            return DevicePreviewScreen.State(
                deviceId = screen.deviceId,
                deviceName = screen.deviceName,
                imageUrl = currentImageUrl,
                downloadState = downloadState,
                refreshState = refreshState,
            ) { event ->
                when (event) {
                    DevicePreviewScreen.Event.BackClicked -> {
                        // Return the device ID and new image URL if it changed during this session
                        val result =
                            DevicePreviewScreen.Result(
                                deviceId = screen.deviceId,
                                newImageUrl = if (currentImageUrl != screen.imageUrl) currentImageUrl else null,
                            )
                        navigator.pop(result)
                    }
                    DevicePreviewScreen.Event.DownloadImageClicked -> {
                        if (downloadState !is DevicePreviewScreen.DownloadState.Downloading) {
                            downloadState = DevicePreviewScreen.DownloadState.Downloading
                        }
                    }
                    DevicePreviewScreen.Event.RefreshImageClicked -> {
                        if (refreshState !is DevicePreviewScreen.RefreshState.Refreshing) {
                            refreshState = DevicePreviewScreen.RefreshState.Refreshing
                            scope.launch {
                                val deviceToken = deviceTokenRepository.getDeviceToken(screen.deviceId)
                                if (deviceToken != null) {
                                    when (val result = apiService.getDisplayCurrent(deviceToken)) {
                                        is ApiResult.Success -> {
                                            val newImageUrl = result.value.imageUrl
                                            if (newImageUrl != null) {
                                                currentImageUrl = newImageUrl
                                                refreshState =
                                                    DevicePreviewScreen.RefreshState.Success(
                                                        newImageUrl = newImageUrl,
                                                        message = "Preview image refreshed successfully",
                                                    )
                                            } else {
                                                refreshState =
                                                    DevicePreviewScreen.RefreshState.Error(
                                                        message = "No preview image available",
                                                    )
                                            }
                                        }
                                        is ApiResult.Failure.HttpFailure -> {
                                            val errorMessage =
                                                when (result.code) {
                                                    429 -> "Too many requests. Please try again later."
                                                    500 -> "Server error occurred. Please try again later."
                                                    401 -> "Unauthorized. Please check device API key."
                                                    else -> "Failed to refresh image (HTTP ${result.code})"
                                                }
                                            refreshState = DevicePreviewScreen.RefreshState.Error(message = errorMessage)
                                        }
                                        is ApiResult.Failure.NetworkFailure -> {
                                            refreshState =
                                                DevicePreviewScreen.RefreshState.Error(
                                                    message = "Network error. Please check your connection.",
                                                )
                                        }
                                        is ApiResult.Failure.ApiFailure -> {
                                            refreshState =
                                                DevicePreviewScreen.RefreshState.Error(
                                                    message = "API error: ${result.error}",
                                                )
                                        }
                                        is ApiResult.Failure.UnknownFailure -> {
                                            refreshState =
                                                DevicePreviewScreen.RefreshState.Error(
                                                    message = "Unknown error occurred",
                                                )
                                        }
                                    }
                                } else {
                                    refreshState =
                                        DevicePreviewScreen.RefreshState.Error(
                                            message = "Device API key not found. Please configure it in settings.",
                                        )
                                }
                            }
                        }
                    }
                    DevicePreviewScreen.Event.DismissSnackbar -> {
                        downloadState = DevicePreviewScreen.DownloadState.Idle
                    }
                    DevicePreviewScreen.Event.DismissRefreshSnackbar -> {
                        refreshState = DevicePreviewScreen.RefreshState.Idle
                    }
                }
            }
        }

        @CircuitInject(DevicePreviewScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: DevicePreviewScreen,
                navigator: Navigator,
            ): DevicePreviewPresenter
        }
    }

/**
 * UI content for DevicePreviewScreen.
 * Displays the device preview image in full-screen with shared element transition.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@CircuitInject(DevicePreviewScreen::class, AppScope::class)
@Composable
fun DevicePreviewContent(
    state: DevicePreviewScreen.State,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle image download when triggered
    LaunchedEffect(state.downloadState) {
        when (val downloadState = state.downloadState) {
            is DevicePreviewScreen.DownloadState.Downloading -> {
                val result =
                    ImageDownloadUtils.downloadImage(
                        context = context,
                        imageUrl = state.imageUrl,
                        fileName = state.deviceName.replace(" ", "_"),
                    )
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Image saved to your Pictures directory.\nDownloaded image is ready to view and share!")
                } else {
                    snackbarHostState.showSnackbar("Failed to save image to your Pictures directory.")
                }
                state.eventSink(DevicePreviewScreen.Event.DismissSnackbar)
            }
            is DevicePreviewScreen.DownloadState.Success -> {
                snackbarHostState.showSnackbar(downloadState.message)
                state.eventSink(DevicePreviewScreen.Event.DismissSnackbar)
            }
            is DevicePreviewScreen.DownloadState.Error -> {
                snackbarHostState.showSnackbar(downloadState.message)
                state.eventSink(DevicePreviewScreen.Event.DismissSnackbar)
            }
            DevicePreviewScreen.DownloadState.Idle -> {
                // No action needed
            }
        }
    }

    // Handle refresh state with snackbar messages
    LaunchedEffect(state.refreshState) {
        when (val refreshState = state.refreshState) {
            is DevicePreviewScreen.RefreshState.Success -> {
                snackbarHostState.showSnackbar(refreshState.message)
                state.eventSink(DevicePreviewScreen.Event.DismissRefreshSnackbar)
            }
            is DevicePreviewScreen.RefreshState.Error -> {
                snackbarHostState.showSnackbar(refreshState.message)
                state.eventSink(DevicePreviewScreen.Event.DismissRefreshSnackbar)
            }
            DevicePreviewScreen.RefreshState.Idle,
            DevicePreviewScreen.RefreshState.Refreshing,
            -> {
                // No action needed
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle(state.deviceName) },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DevicePreviewScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { state.eventSink(DevicePreviewScreen.Event.RefreshImageClicked) },
                        enabled = state.refreshState !is DevicePreviewScreen.RefreshState.Refreshing,
                    ) {
                        if (state.refreshState is DevicePreviewScreen.RefreshState.Refreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                contentDescription = "Refresh preview image",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    // Download button
                    IconButton(
                        onClick = { state.eventSink(DevicePreviewScreen.Event.DownloadImageClicked) },
                        enabled = state.downloadState !is DevicePreviewScreen.DownloadState.Downloading,
                    ) {
                        if (state.downloadState is DevicePreviewScreen.DownloadState.Downloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.download_photo_outline),
                                contentDescription = "Download image",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            SharedElementTransitionScope {
                ZoomableAsyncImage(
                    model = state.imageUrl,
                    contentDescription = "Full screen preview for ${state.deviceName}",
                    modifier =
                        Modifier
                            .sharedElement(
                                sharedContentState =
                                    rememberSharedContentState(
                                        key = DevicePreviewImageKey(deviceId = state.deviceId),
                                    ),
                                animatedVisibilityScope = requireAnimatedScope(Navigation),
                            ).fillMaxSize(),
                    state = rememberZoomableImageState(rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 4f))),
                )
            }
        }
    }
}
