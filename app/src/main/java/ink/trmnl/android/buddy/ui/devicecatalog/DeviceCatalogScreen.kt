package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.api.models.DeviceKind
import ink.trmnl.android.buddy.api.models.DeviceModel
import kotlinx.parcelize.Parcelize

/**
 * Device Catalog screen that displays all supported TRMNL device models.
 *
 * Shows a filterable list of device models including official TRMNL devices,
 * Amazon Kindle e-readers, and BYOD (Bring Your Own Device) compatible displays.
 */
@Parcelize
data object DeviceCatalogScreen : Screen {
    /**
     * UI State for the Device Catalog screen.
     *
     * @property devices List of all device models
     * @property filteredDevices Filtered list based on selected filter
     * @property selectedFilter Currently selected device kind filter (null = All)
     * @property isLoading Whether data is currently being loaded
     * @property error Error message if loading failed
     * @property eventSink Event handler for user interactions
     */
    data class State(
        val devices: List<DeviceModel> = emptyList(),
        val filteredDevices: List<DeviceModel> = emptyList(),
        val selectedFilter: DeviceKind? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState {
        /**
         * Get count of devices for a specific kind.
         */
        fun getCountForKind(kind: DeviceKind?): Int =
            when (kind) {
                null -> devices.size
                else -> devices.count { it.kind == kind.name.lowercase() }
            }
    }

    /**
     * Events that can occur in the Device Catalog screen.
     */
    sealed class Event : CircuitUiEvent {
        /** Back button was clicked. */
        data object BackClicked : Event()

        /** A device kind filter was selected. */
        data class FilterSelected(
            val kind: DeviceKind?,
        ) : Event()

        /** A device item was clicked. */
        data class DeviceClicked(
            val device: DeviceModel,
        ) : Event()

        /** Retry button was clicked after an error. */
        data object RetryClicked : Event()
    }
}
