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
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.sharedelements.DevicePreviewImageKey
import ink.trmnl.android.buddy.util.ImageDownloadUtils
import kotlinx.parcelize.Parcelize

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
    data class State(
        val deviceId: String,
        val deviceName: String,
        val imageUrl: String,
        val snackbarMessage: String? = null,
        val isDownloading: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object DownloadImageClicked : Event()

        data object DismissSnackbar : Event()
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
    ) : Presenter<DevicePreviewScreen.State> {
        @Composable
        override fun present(): DevicePreviewScreen.State {
            var snackbarMessage by rememberRetained { mutableStateOf<String?>(null) }
            var isDownloading by rememberRetained { mutableStateOf(false) }

            return DevicePreviewScreen.State(
                deviceId = screen.deviceId,
                deviceName = screen.deviceName,
                imageUrl = screen.imageUrl,
                snackbarMessage = snackbarMessage,
                isDownloading = isDownloading,
            ) { event ->
                when (event) {
                    DevicePreviewScreen.Event.BackClicked -> navigator.pop()
                    DevicePreviewScreen.Event.DownloadImageClicked -> {
                        if (!isDownloading) {
                            isDownloading = true
                            snackbarMessage = "downloading"
                        }
                    }
                    DevicePreviewScreen.Event.DismissSnackbar -> {
                        snackbarMessage = null
                        isDownloading = false
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
    LaunchedEffect(state.snackbarMessage) {
        if (state.snackbarMessage == "downloading") {
            val result =
                ImageDownloadUtils.downloadImage(
                    context = context,
                    imageUrl = state.imageUrl,
                    fileName = state.deviceName.replace(" ", "_"),
                )
            val message =
                if (result.isSuccess) {
                    "Image saved to Pictures"
                } else {
                    "Failed to save image"
                }
            state.eventSink(DevicePreviewScreen.Event.DismissSnackbar)
            snackbarHostState.showSnackbar(message)
        } else if (state.snackbarMessage != null) {
            snackbarHostState.showSnackbar(state.snackbarMessage)
            state.eventSink(DevicePreviewScreen.Event.DismissSnackbar)
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
                    IconButton(
                        onClick = { state.eventSink(DevicePreviewScreen.Event.DownloadImageClicked) },
                        enabled = !state.isDownloading,
                    ) {
                        if (state.isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.image_inset_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Download image",
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Black,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            SharedElementTransitionScope {
                SubcomposeAsyncImage(
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
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Failed to load image",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    },
                )
            }
        }
    }
}
