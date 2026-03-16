package ink.trmnl.android.buddy.ui.devicepreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
