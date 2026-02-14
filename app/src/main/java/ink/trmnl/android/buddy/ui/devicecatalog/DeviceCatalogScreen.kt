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
     * @property selectedDevice Device currently shown in bottom sheet (null = bottom sheet hidden)
     * @property eventSink Event handler for user interactions
     */
    data class State(
        val devices: List<DeviceModel> = emptyList(),
        val selectedFilter: DeviceKind? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedDevice: DeviceModel? = null,
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

        /**
         * User dismissed the device details bottom sheet.
         */
        data object DismissBottomSheet : Event()
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
     * SEEED Studio devices only using device name id `seeed_` (kind = "byod").
     * - https://www.seeedstudio.com/
     * - https://www.seeedstudio.com/blog/2025/07/21/it-is-here-introducing-the-trmnl-7-5-og-diy-kit/
     */
    SEEED_STUDIO,

    /**
     * Kobo e-readers using name id `kobo_` (kind = "byod").
     */
    KOBO,

    /**
     * Onyx BOOX e-readers, tablets, and monitors using name id `boox_` (kind = "byod").
     * - https://www.boox.com/
     */
    BOOX,

    /**
     * Bring Your Own Device / Third-party (kind = "byod").
     */
    BYOD,
}
