package ink.trmnl.android.buddy.ui.devicecatalog

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.api.models.DeviceModel
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying catalog of supported device models.
 *
 * Shows all supported TRMNL e-ink device models with filtering capabilities
 * by device kind (TRMNL, Kindle, BYOD).
 */
@Parcelize
data object DeviceCatalogScreen : Screen {
    /**
     * UI state for the device catalog screen.
     *
     * @property devices List of all device models
     * @property selectedFilter Currently selected device kind filter (null = "All")
     * @property isLoading Whether data is being loaded from API
     * @property error Error message if API call failed
     * @property eventSink Event handler for user interactions
     */
    data class State(
        val devices: List<DeviceModel> = emptyList(),
        val selectedFilter: DeviceKind? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Events that can occur in the device catalog screen.
     */
    sealed class Event : CircuitUiEvent {
        /**
         * User clicked the back button.
         */
        data object BackClicked : Event()

        /**
         * User selected a device kind filter.
         *
         * @property kind Device kind to filter by, or null for "All"
         */
        data class FilterSelected(
            val kind: DeviceKind?,
        ) : Event()

        /**
         * User clicked on a device to view details.
         *
         * @property device The device model that was clicked
         */
        data class DeviceClicked(
            val device: DeviceModel,
        ) : Event()

        /**
         * User clicked retry after an error.
         */
        data object RetryClicked : Event()
    }
}

/**
 * Device kind categories for filtering.
 */
enum class DeviceKind {
    /**
     * Official TRMNL devices (kind = "trmnl").
     */
    TRMNL,

    /**
     * Amazon Kindle e-readers (kind = "kindle").
     */
    KINDLE,

    /**
     * Bring Your Own Device / Third-party (kind = "byod").
     */
    BYOD,
}
