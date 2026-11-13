package ink.trmnl.android.buddy.ui.devicediagnostics

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import ink.trmnl.android.buddy.api.models.DeviceTimeline
import ink.trmnl.android.buddy.api.models.TimelineStep
import kotlinx.parcelize.Parcelize

/**
 * Screen for viewing device check-in forecast, activity timeline, and diagnostic logs.
 */
@Parcelize
data class DeviceDiagnosticsScreen(
    val deviceId: Int,
    val deviceName: String,
) : ParcelableScreen {
    enum class DiagnosticsTab(
        val label: String,
    ) {
        FORECAST("Forecast"),
        TIMELINE("Timeline"),
        LOGS("Logs"),
    }

    enum class LogLevelFilter(
        val label: String,
        val apiValue: String?,
    ) {
        ALL("All", null),
        ERROR("Error", "error"),
        WARN("Warn", "warn"),
        INFO("Info", "info"),
        DEBUG("Debug", "debug"),
    }

    data class DeviceLogEntry(
        val id: String,
        val timestamp: String,
        val level: String?,
        val source: String?,
        val message: String,
        val details: String? = null,
    )

    sealed interface DiagnosticsError {
        val message: String

        data class General(
            override val message: String,
        ) : DiagnosticsError

        data class InsufficientKeyCapabilities(
            override val message: String,
            val accountUrl: String = TRMNL_ACCOUNT_URL,
        ) : DiagnosticsError
    }

    data class State(
        val deviceName: String,
        val selectedTab: DiagnosticsTab = DiagnosticsTab.FORECAST,
        val forecastHours: Int = 12,
        val isForecastLoading: Boolean = true,
        val forecastError: DiagnosticsError? = null,
        val forecastSteps: List<TimelineStep> = emptyList(),
        val timelineHours: Int = 12,
        val isTimelineLoading: Boolean = false,
        val timelineError: DiagnosticsError? = null,
        val timeline: DeviceTimeline? = null,
        val selectedLogLevel: LogLevelFilter = LogLevelFilter.ALL,
        val isLogsLoading: Boolean = false,
        val isLoadingMoreLogs: Boolean = false,
        val logsError: DiagnosticsError? = null,
        val logs: List<DeviceLogEntry> = emptyList(),
        val hasMoreLogs: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object BackClicked : Event

        data object RefreshClicked : Event

        data object UpdateApiKeyClicked : Event

        data class TabSelected(
            val tab: DiagnosticsTab,
        ) : Event

        data class ForecastHoursSelected(
            val hours: Int,
        ) : Event

        data class TimelineHoursSelected(
            val hours: Int,
        ) : Event

        data class LogLevelSelected(
            val level: LogLevelFilter,
        ) : Event

        data object LoadMoreLogs : Event
    }

    companion object {
        const val TRMNL_ACCOUNT_URL = "https://trmnl.com/account"
    }
}
