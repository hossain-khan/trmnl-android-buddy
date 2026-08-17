package ink.trmnl.android.buddy.ui.devicepreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.util.toUserMessage
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import kotlinx.coroutines.launch

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
            var isConfigured by rememberRetained { mutableStateOf(false) }

            LaunchedEffect(screen.deviceId) {
                isConfigured = deviceTokenRepository.hasDeviceToken(screen.deviceId)
            }

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

            var loadNextState by rememberRetained {
                mutableStateOf<DevicePreviewScreen.LoadNextState>(
                    DevicePreviewScreen.LoadNextState.Idle,
                )
            }

            var cachedImages by rememberRetained { mutableStateOf(listOf(screen.imageUrl)) }
            var currentImageIndex by rememberRetained { mutableIntStateOf(0) }

            val currentImageUrl = cachedImages.getOrElse(currentImageIndex) { screen.imageUrl }
            val scope = rememberCoroutineScope()

            return DevicePreviewScreen.State(
                deviceId = screen.deviceId,
                deviceName = screen.deviceName,
                imageUrl = currentImageUrl,
                isConfigured = isConfigured,
                currentImageIndex = currentImageIndex,
                totalImages = cachedImages.size,
                canGoPrevious = currentImageIndex > 0,
                canGoNext = isConfigured,
                isLoadingNext = loadNextState is DevicePreviewScreen.LoadNextState.Loading,
                downloadState = downloadState,
                refreshState = refreshState,
                loadNextState = loadNextState,
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
                    DevicePreviewScreen.Event.PreviousImageClicked -> {
                        if (currentImageIndex > 0) {
                            currentImageIndex--
                        }
                    }
                    DevicePreviewScreen.Event.NextImageClicked -> {
                        if (currentImageIndex < cachedImages.lastIndex) {
                            currentImageIndex++
                        } else if (isConfigured && loadNextState !is DevicePreviewScreen.LoadNextState.Loading) {
                            loadNextState = DevicePreviewScreen.LoadNextState.Loading
                            scope.launch {
                                val token = deviceTokenRepository.getDeviceToken(screen.deviceId)
                                if (token != null) {
                                    when (val result = apiService.getDisplay(token)) {
                                        is ApiResult.Success -> {
                                            val newImageUrl = result.value.imageUrl
                                            val display = result.value
                                            if (newImageUrl != null) {
                                                val isSleeping =
                                                    display.filename?.equals("sleep", ignoreCase = true) == true ||
                                                        display.specialFunction?.equals("sleep", ignoreCase = true) == true
                                                val isSameScreen =
                                                    normalizeImageUrl(newImageUrl) == normalizeImageUrl(currentImageUrl)

                                                if (isSleeping) {
                                                    loadNextState =
                                                        DevicePreviewScreen.LoadNextState.Error(
                                                            message = "Device is in sleep mode. No new screen available.",
                                                        )
                                                } else if (isSameScreen) {
                                                    loadNextState =
                                                        DevicePreviewScreen.LoadNextState.Error(
                                                            message =
                                                                "No new screen to display. " +
                                                                    "Device may have only one screen in rotation.",
                                                        )
                                                } else {
                                                    cachedImages = cachedImages + newImageUrl
                                                    currentImageIndex = cachedImages.lastIndex
                                                    loadNextState = DevicePreviewScreen.LoadNextState.Idle
                                                }
                                            } else {
                                                loadNextState =
                                                    DevicePreviewScreen.LoadNextState.Error(
                                                        message = "No display image returned",
                                                    )
                                            }
                                        }
                                        is ApiResult.Failure -> {
                                            loadNextState =
                                                DevicePreviewScreen.LoadNextState.Error(
                                                    message = result.toUserMessage(),
                                                )
                                        }
                                    }
                                } else {
                                    loadNextState =
                                        DevicePreviewScreen.LoadNextState.Error(
                                            message = "Device API key not found. Please configure it in settings.",
                                        )
                                }
                            }
                        }
                    }
                    DevicePreviewScreen.Event.RefreshImageClicked -> {
                        if (refreshState !is DevicePreviewScreen.RefreshState.Refreshing) {
                            refreshState = DevicePreviewScreen.RefreshState.Refreshing
                            scope.launch {
                                val token = deviceTokenRepository.getDeviceToken(screen.deviceId)
                                if (token != null) {
                                    when (val result = apiService.getDisplayCurrent(token)) {
                                        is ApiResult.Success -> {
                                            val newImageUrl = result.value.imageUrl
                                            if (newImageUrl != null) {
                                                val updatedList = cachedImages.toMutableList()
                                                updatedList[currentImageIndex] = newImageUrl
                                                cachedImages = updatedList
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
                                        is ApiResult.Failure -> {
                                            refreshState =
                                                DevicePreviewScreen.RefreshState.Error(
                                                    message = result.toUserMessage(),
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
                    DevicePreviewScreen.Event.DismissLoadNextSnackbar -> {
                        loadNextState = DevicePreviewScreen.LoadNextState.Idle
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
 * Normalizes an image URL by stripping dynamic query parameters to compare the underlying image asset.
 *
 * TRMNL device display API responses (`GET /api/display` and `GET /api/display/current`) provide
 * pre-signed cloud storage URLs (e.g., AWS S3 or DigitalOcean Spaces) that contain dynamic timestamp,
 * credential, and cryptographic signature query parameters (such as `X-Amz-Date`, `X-Amz-Signature`, `X-Amz-Expires`).
 * Because these query parameters change on every HTTP request even when the underlying image has not changed,
 * comparing full URL strings directly falsely indicates that a new image was generated.
 *
 * Example:
 * ```
 * Full URL:
 *   https://trmnl-screens.nyc3.digitaloceanspaces.com/vsgnimt6q3z4giloww5cmmb7iw0s?response-content-disposition=inline&X-Amz-Date=20260817T072931Z&X-Amz-Signature=abc123
 *
 * Normalized:
 *   https://trmnl-screens.nyc3.digitaloceanspaces.com/vsgnimt6q3z4giloww5cmmb7iw0s
 * ```
 *
 * @param url The raw image URL string.
 * @return The base URL string without query parameters.
 */
internal fun normalizeImageUrl(url: String): String = url.substringBefore('?')
