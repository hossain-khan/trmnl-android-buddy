package ink.trmnl.android.buddy.ui.devicepreview

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.sharedelements.DevicePreviewImageKey
import ink.trmnl.android.buddy.util.ImageDownloadUtils
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

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
                val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 4f))

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
                            ).fillMaxSize()
                            .zoomable(zoomableState),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Failed to load image",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    },
                )
            }
        }
    }
}

// Preview Composables
// Note: Full preview not possible due to SharedElementTransitionScope requirement
// Individual components can be previewed in isolation if needed
