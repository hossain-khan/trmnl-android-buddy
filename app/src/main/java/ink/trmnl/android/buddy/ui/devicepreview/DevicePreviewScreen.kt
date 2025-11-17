package ink.trmnl.android.buddy.ui.devicepreview

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
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
