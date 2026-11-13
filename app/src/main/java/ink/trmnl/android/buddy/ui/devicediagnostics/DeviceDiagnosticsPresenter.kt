package ink.trmnl.android.buddy.ui.devicediagnostics

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
import dev.zacsweers.metro.AssistedInject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.DeviceLogCursor
import ink.trmnl.android.buddy.api.models.DeviceTimeline
import ink.trmnl.android.buddy.api.models.TimelineStep
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Presenter for [DeviceDiagnosticsScreen] managing forecast, timeline, and logs state.
 */
@AssistedInject
class DeviceDiagnosticsPresenter
    constructor(
        @Assisted private val screen: DeviceDiagnosticsScreen,
        @Assisted private val navigator: Navigator,
        private val apiService: TrmnlApiService,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<DeviceDiagnosticsScreen.State> {
        @Composable
        override fun present(): DeviceDiagnosticsScreen.State {
            var selectedTab by rememberRetained { mutableStateOf(DeviceDiagnosticsScreen.DiagnosticsTab.FORECAST) }

            var forecastHours by rememberRetained { mutableIntStateOf(12) }
            var isForecastLoading by rememberRetained { mutableStateOf(true) }
            var forecastError by rememberRetained { mutableStateOf<DeviceDiagnosticsScreen.DiagnosticsError?>(null) }
            var forecastSteps by rememberRetained { mutableStateOf<List<TimelineStep>>(emptyList()) }
            var forecastRefreshTrigger by rememberRetained { mutableIntStateOf(0) }

            var timelineHours by rememberRetained { mutableIntStateOf(12) }
            var isTimelineLoading by rememberRetained { mutableStateOf(false) }
            var timelineError by rememberRetained { mutableStateOf<DeviceDiagnosticsScreen.DiagnosticsError?>(null) }
            var timeline by rememberRetained { mutableStateOf<DeviceTimeline?>(null) }
            var hasLoadedTimeline by rememberRetained { mutableStateOf(false) }
            var timelineRefreshTrigger by rememberRetained { mutableIntStateOf(0) }

            var selectedLogLevel by rememberRetained { mutableStateOf(DeviceDiagnosticsScreen.LogLevelFilter.ALL) }
            var isLogsLoading by rememberRetained { mutableStateOf(false) }
            var isLoadingMoreLogs by rememberRetained { mutableStateOf(false) }
            var logsError by rememberRetained { mutableStateOf<DeviceDiagnosticsScreen.DiagnosticsError?>(null) }
            var logs by rememberRetained { mutableStateOf<List<DeviceDiagnosticsScreen.DeviceLogEntry>>(emptyList()) }
            var oldestCursor by rememberRetained { mutableStateOf<DeviceLogCursor?>(null) }
            var hasMoreLogs by rememberRetained { mutableStateOf(false) }
            var hasLoadedLogs by rememberRetained { mutableStateOf(false) }
            var logsRefreshTrigger by rememberRetained { mutableIntStateOf(0) }

            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(screen.deviceId, forecastHours, forecastRefreshTrigger) {
                isForecastLoading = true
                forecastError = null
                try {
                    val token = userPreferencesRepository.userPreferencesFlow.firstOrNull()?.apiToken
                    if (token.isNullOrBlank()) {
                        forecastError = DeviceDiagnosticsScreen.DiagnosticsError.General("Missing API token. Please sign in again.")
                        return@LaunchedEffect
                    }
                    when (
                        val result =
                            withContext(Dispatchers.IO) {
                                apiService.getDeviceForecast(
                                    authorization = "Bearer $token",
                                    deviceId = screen.deviceId,
                                    hours = forecastHours,
                                )
                            }
                    ) {
                        is ApiResult.Success -> {
                            forecastSteps = result.value.data.steps
                        }

                        is ApiResult.Failure -> {
                            forecastError = formatApiFailure("forecast", result)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load device forecast")
                    forecastError = DeviceDiagnosticsScreen.DiagnosticsError.General(e.message ?: "Failed to load forecast")
                } finally {
                    isForecastLoading = false
                }
            }

            LaunchedEffect(selectedTab, screen.deviceId, timelineHours, timelineRefreshTrigger) {
                if (selectedTab != DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE && !hasLoadedTimeline) {
                    return@LaunchedEffect
                }
                if (selectedTab != DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE) {
                    return@LaunchedEffect
                }
                isTimelineLoading = true
                timelineError = null
                try {
                    val token = userPreferencesRepository.userPreferencesFlow.firstOrNull()?.apiToken
                    if (token.isNullOrBlank()) {
                        timelineError = DeviceDiagnosticsScreen.DiagnosticsError.General("Missing API token. Please sign in again.")
                        return@LaunchedEffect
                    }
                    when (
                        val result =
                            withContext(Dispatchers.IO) {
                                apiService.getDeviceTimeline(
                                    authorization = "Bearer $token",
                                    deviceId = screen.deviceId,
                                    hours = timelineHours,
                                )
                            }
                    ) {
                        is ApiResult.Success -> {
                            timeline = result.value.data
                            hasLoadedTimeline = true
                        }

                        is ApiResult.Failure -> {
                            timelineError = formatApiFailure("timeline", result)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load device timeline")
                    timelineError = DeviceDiagnosticsScreen.DiagnosticsError.General(e.message ?: "Failed to load timeline")
                } finally {
                    isTimelineLoading = false
                }
            }

            LaunchedEffect(selectedTab, screen.deviceId, selectedLogLevel, logsRefreshTrigger) {
                if (selectedTab != DeviceDiagnosticsScreen.DiagnosticsTab.LOGS) {
                    return@LaunchedEffect
                }
                isLogsLoading = true
                logsError = null
                try {
                    val token = userPreferencesRepository.userPreferencesFlow.firstOrNull()?.apiToken
                    if (token.isNullOrBlank()) {
                        logsError = DeviceDiagnosticsScreen.DiagnosticsError.General("Missing API token. Please sign in again.")
                        return@LaunchedEffect
                    }
                    when (
                        val result =
                            withContext(Dispatchers.IO) {
                                apiService.getDeviceLogs(
                                    authorization = "Bearer $token",
                                    deviceId = screen.deviceId,
                                    level = selectedLogLevel.apiValue,
                                    limit = DEFAULT_LOGS_LIMIT,
                                )
                            }
                    ) {
                        is ApiResult.Success -> {
                            logs =
                                result.value.data.logs
                                    .mapIndexed { index, obj -> parseLogEntry(obj, index) }
                            oldestCursor = result.value.data.oldestCursor
                            hasMoreLogs = result.value.data.more && result.value.data.oldestCursor != null
                            hasLoadedLogs = true
                        }

                        is ApiResult.Failure -> {
                            logsError = formatApiFailure("logs", result)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load device logs")
                    logsError = DeviceDiagnosticsScreen.DiagnosticsError.General(e.message ?: "Failed to load logs")
                } finally {
                    isLogsLoading = false
                }
            }

            return DeviceDiagnosticsScreen.State(
                deviceName = screen.deviceName,
                selectedTab = selectedTab,
                forecastHours = forecastHours,
                isForecastLoading = isForecastLoading,
                forecastError = forecastError,
                forecastSteps = forecastSteps,
                timelineHours = timelineHours,
                isTimelineLoading = isTimelineLoading,
                timelineError = timelineError,
                timeline = timeline,
                selectedLogLevel = selectedLogLevel,
                isLogsLoading = isLogsLoading,
                isLoadingMoreLogs = isLoadingMoreLogs,
                logsError = logsError,
                logs = logs,
                hasMoreLogs = hasMoreLogs,
            ) { event ->
                when (event) {
                    DeviceDiagnosticsScreen.Event.BackClicked -> {
                        navigator.pop()
                    }

                    DeviceDiagnosticsScreen.Event.RefreshClicked -> {
                        when (selectedTab) {
                            DeviceDiagnosticsScreen.DiagnosticsTab.FORECAST -> forecastRefreshTrigger++
                            DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE -> timelineRefreshTrigger++
                            DeviceDiagnosticsScreen.DiagnosticsTab.LOGS -> logsRefreshTrigger++
                        }
                    }

                    DeviceDiagnosticsScreen.Event.UpdateApiKeyClicked -> {
                        navigator.goTo(AccessTokenScreen)
                    }

                    is DeviceDiagnosticsScreen.Event.TabSelected -> {
                        selectedTab = event.tab
                        if (event.tab == DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE && !hasLoadedTimeline) {
                            timelineRefreshTrigger++
                        } else if (event.tab == DeviceDiagnosticsScreen.DiagnosticsTab.LOGS && !hasLoadedLogs) {
                            logsRefreshTrigger++
                        }
                    }

                    is DeviceDiagnosticsScreen.Event.ForecastHoursSelected -> {
                        if (forecastHours != event.hours) {
                            forecastHours = event.hours
                            isForecastLoading = true
                        }
                    }

                    is DeviceDiagnosticsScreen.Event.TimelineHoursSelected -> {
                        if (timelineHours != event.hours) {
                            timelineHours = event.hours
                            isTimelineLoading = true
                        }
                    }

                    is DeviceDiagnosticsScreen.Event.LogLevelSelected -> {
                        if (selectedLogLevel != event.level) {
                            selectedLogLevel = event.level
                            isLogsLoading = true
                        }
                    }

                    DeviceDiagnosticsScreen.Event.LoadMoreLogs -> {
                        val cursor = oldestCursor
                        if (cursor != null && !isLoadingMoreLogs && !isLogsLoading) {
                            coroutineScope.launch {
                                isLoadingMoreLogs = true
                                try {
                                    val token = userPreferencesRepository.userPreferencesFlow.firstOrNull()?.apiToken
                                    if (token.isNullOrBlank()) {
                                        logsError =
                                            DeviceDiagnosticsScreen.DiagnosticsError.General("Missing API token. Please sign in again.")
                                        return@launch
                                    }
                                    when (
                                        val result =
                                            withContext(Dispatchers.IO) {
                                                apiService.getDeviceLogs(
                                                    authorization = "Bearer $token",
                                                    deviceId = screen.deviceId,
                                                    level = selectedLogLevel.apiValue,
                                                    limit = DEFAULT_LOGS_LIMIT,
                                                    beforeTs = cursor.ts,
                                                    beforeEventId = cursor.eventId,
                                                )
                                            }
                                    ) {
                                        is ApiResult.Success -> {
                                            val startIndex = logs.size
                                            val newEntries =
                                                result.value.data.logs.mapIndexed { idx, obj ->
                                                    parseLogEntry(obj, startIndex + idx)
                                                }
                                            logs = logs + newEntries
                                            oldestCursor = result.value.data.oldestCursor
                                            hasMoreLogs = result.value.data.more && result.value.data.oldestCursor != null
                                        }

                                        is ApiResult.Failure -> {
                                            logsError = formatApiFailure("older logs", result)
                                        }
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to load more device logs")
                                    logsError = DeviceDiagnosticsScreen.DiagnosticsError.General(e.message ?: "Failed to load older logs")
                                } finally {
                                    isLoadingMoreLogs = false
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun formatApiFailure(
            target: String,
            failure: ApiResult.Failure<ApiError>,
        ): DeviceDiagnosticsScreen.DiagnosticsError =
            when (failure) {
                is ApiResult.Failure.HttpFailure -> {
                    val serverMessage = failure.error?.error?.takeIf { it.isNotBlank() }
                    if (failure.code == HTTP_FORBIDDEN || isCapabilityError(serverMessage)) {
                        DeviceDiagnosticsScreen.DiagnosticsError.InsufficientKeyCapabilities(
                            message = serverMessage ?: DEFAULT_CAPABILITY_ERROR_MESSAGE,
                            accountUrl = DeviceDiagnosticsScreen.TRMNL_ACCOUNT_URL,
                        )
                    } else {
                        DeviceDiagnosticsScreen.DiagnosticsError.General(
                            serverMessage ?: "Failed to load $target (HTTP ${failure.code})",
                        )
                    }
                }

                is ApiResult.Failure.NetworkFailure -> {
                    DeviceDiagnosticsScreen.DiagnosticsError.General("Network error while loading $target")
                }

                is ApiResult.Failure.ApiFailure -> {
                    val serverMessage = failure.error?.error?.takeIf { it.isNotBlank() }
                    if (isCapabilityError(serverMessage)) {
                        DeviceDiagnosticsScreen.DiagnosticsError.InsufficientKeyCapabilities(
                            message = serverMessage ?: DEFAULT_CAPABILITY_ERROR_MESSAGE,
                            accountUrl = DeviceDiagnosticsScreen.TRMNL_ACCOUNT_URL,
                        )
                    } else {
                        DeviceDiagnosticsScreen.DiagnosticsError.General(serverMessage ?: "Failed to load $target")
                    }
                }

                is ApiResult.Failure.UnknownFailure -> {
                    DeviceDiagnosticsScreen.DiagnosticsError.General("Unexpected error while loading $target")
                }
            }

        private fun isCapabilityError(message: String?): Boolean {
            if (message.isNullOrBlank()) return false
            val lower = message.lowercase()
            return lower.contains("legacy api key") ||
                lower.contains("capabilities") ||
                lower.contains("trmnl.com/account")
        }

        @CircuitInject(DeviceDiagnosticsScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: DeviceDiagnosticsScreen,
                navigator: Navigator,
            ): DeviceDiagnosticsPresenter
        }

        private companion object {
            const val DEFAULT_LOGS_LIMIT = 50
            const val HTTP_FORBIDDEN = 403
            const val DEFAULT_CAPABILITY_ERROR_MESSAGE =
                "Your current API key does not have the required capabilities for this endpoint. " +
                    "Create a new API key with the required capabilities at ${DeviceDiagnosticsScreen.TRMNL_ACCOUNT_URL}."
        }
    }

internal fun parseLogEntry(
    jsonObject: JsonObject,
    index: Int,
): DeviceDiagnosticsScreen.DeviceLogEntry {
    fun stringField(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            jsonObject[key]
                ?.let { element ->
                    runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
                }?.takeIf { it.isNotBlank() }
        }

    val timestamp = stringField("ts", "at", "created_at", "timestamp") ?: "Unknown time"
    val level = stringField("level", "severity")
    val source = stringField("source", "origin", "category")
    val eventId = stringField("event_id", "id") ?: "${timestamp}_$index"
    val message =
        stringField("message", "msg", "event", "name", "summary", "description", "title")
            ?: jsonObject.entries
                .filterNot { it.key in setOf("ts", "at", "created_at", "timestamp", "level", "severity", "source", "event_id", "id") }
                .joinToString(" • ") { "${it.key}: ${it.value}" }
                .ifBlank { jsonObject.toString() }

    val excludedKeys =
        setOf(
            "ts",
            "at",
            "created_at",
            "timestamp",
            "level",
            "severity",
            "source",
            "origin",
            "category",
            "event_id",
            "id",
            "message",
            "msg",
            "event",
            "name",
            "summary",
            "description",
            "title",
        )
    val details =
        jsonObject.entries
            .filterNot { it.key in excludedKeys }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ") { "${it.key}: ${it.value.toString().trim('"')}" }

    return DeviceDiagnosticsScreen.DeviceLogEntry(
        id = eventId,
        timestamp = timestamp,
        level = level,
        source = source,
        message = message,
        details = details,
    )
}
