package ink.trmnl.android.buddy.ui.calendarsync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import ink.trmnl.android.buddy.calendar.repository.CalendarSyncRepositoryInterface
import kotlinx.coroutines.launch

/**
 * Presenter for [CalendarSyncScreen] managing calendar selection and sync state.
 *
 * **Responsibilities:**
 * - Load available calendars from the device (requires READ_CALENDAR permission)
 * - Persist calendar selection changes to [CalendarSyncRepositoryInterface]
 * - Track and update sync status (idle, syncing, error)
 * - Manage sync enabled/disabled state based on calendar selection
 * - Handle disconnect confirmation and cleanup
 * - Coordinate navigation (back)
 *
 * **Permission Handling:**
 * Permission checking is handled in [CalendarSyncContent] using Accompanist Permissions API.
 * The presenter is informed to load calendars via [CalendarSyncScreen.Event.RefreshCalendarsClicked]
 * once permission is granted.
 *
 * **State Management:**
 * - Uses `rememberRetained` for main data (calendars, sync state) to survive config changes
 * - Uses `rememberCoroutineScope` for async operations
 *
 * @property navigator Circuit navigator for screen transitions
 * @property calendarSyncRepository Repository for calendar sync operations
 *
 * @see CalendarSyncScreen Screen definition with State and Event sealed classes
 * @see CalendarSyncContent UI rendering with Accompanist permission handling
 */
@Inject
class CalendarSyncPresenter(
    @Assisted private val navigator: Navigator,
    private val calendarSyncRepository: CalendarSyncRepositoryInterface,
) : Presenter<CalendarSyncScreen.State> {
    @Composable
    override fun present(): CalendarSyncScreen.State {
        var isLoading by rememberRetained { mutableStateOf(false) }
        var calendars by rememberRetained { mutableStateOf<List<SyncCalendar>>(emptyList()) }
        var isSyncEnabled by rememberRetained { mutableStateOf(false) }
        var lastSyncTime by rememberRetained { mutableStateOf(0L) }
        var lastSyncError by rememberRetained { mutableStateOf<String?>(null) }
        var syncStatus by rememberRetained { mutableStateOf(CalendarSyncScreen.SyncStatus.Idle) }
        var showDisconnectConfirmDialog by rememberRetained { mutableStateOf(false) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        // Load initial sync state from repository on first composition
        LaunchedEffect(Unit) {
            isSyncEnabled = calendarSyncRepository.isSyncEnabled()
            lastSyncTime = calendarSyncRepository.getLastSyncTime()
            lastSyncError = calendarSyncRepository.getLastSyncError()
            if (lastSyncError != null) {
                syncStatus = CalendarSyncScreen.SyncStatus.Error
            }
        }

        return CalendarSyncScreen.State(
            isLoading = isLoading,
            calendars = calendars,
            isSyncEnabled = isSyncEnabled,
            lastSyncTime = lastSyncTime,
            lastSyncError = lastSyncError,
            syncStatus = syncStatus,
            showDisconnectConfirmDialog = showDisconnectConfirmDialog,
            errorMessage = errorMessage,
        ) { event ->
            when (event) {
                CalendarSyncScreen.Event.BackClicked -> navigator.pop()

                is CalendarSyncScreen.Event.CalendarToggled -> {
                    coroutineScope.launch {
                        // Update local state immediately for responsive UI
                        val updatedCalendars =
                            calendars.map { calendar ->
                                if (calendar.calendarId == event.calendarId) {
                                    calendar.copy(isSelected = event.selected)
                                } else {
                                    calendar
                                }
                            }
                        calendars = updatedCalendars

                        // Persist selection changes
                        val selectedIds = updatedCalendars.filter { it.isSelected }.map { it.calendarId }
                        calendarSyncRepository.updateCalendarSelection(selectedIds)

                        // Auto-enable sync when calendars are selected, disable when none selected
                        val shouldEnableSync = selectedIds.isNotEmpty()
                        calendarSyncRepository.setSyncEnabled(shouldEnableSync)
                        isSyncEnabled = shouldEnableSync
                    }
                }

                CalendarSyncScreen.Event.SyncNowClicked -> {
                    coroutineScope.launch {
                        try {
                            syncStatus = CalendarSyncScreen.SyncStatus.Syncing
                            errorMessage = null
                            // Fetch events to verify calendar access
                            calendarSyncRepository.getEventsForSync()
                            calendarSyncRepository.recordSyncSuccess()
                            lastSyncTime = calendarSyncRepository.getLastSyncTime()
                            lastSyncError = null
                            syncStatus = CalendarSyncScreen.SyncStatus.Idle
                        } catch (e: Exception) {
                            val errorMsg = e.message ?: "Unknown sync error"
                            calendarSyncRepository.recordSyncError(errorMsg)
                            lastSyncError = calendarSyncRepository.getLastSyncError()
                            syncStatus = CalendarSyncScreen.SyncStatus.Error
                            errorMessage = errorMsg
                        }
                    }
                }

                CalendarSyncScreen.Event.RefreshCalendarsClicked -> {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            calendars = calendarSyncRepository.getAvailableCalendars()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to load calendars"
                        } finally {
                            isLoading = false
                        }
                    }
                }

                CalendarSyncScreen.Event.DisconnectClicked -> {
                    showDisconnectConfirmDialog = true
                }

                CalendarSyncScreen.Event.DisconnectConfirmed -> {
                    calendarSyncRepository.disconnect()
                    calendars = emptyList()
                    isSyncEnabled = false
                    lastSyncTime = 0L
                    lastSyncError = null
                    syncStatus = CalendarSyncScreen.SyncStatus.Idle
                    showDisconnectConfirmDialog = false
                    navigator.pop()
                }

                CalendarSyncScreen.Event.DisconnectDismissed -> {
                    showDisconnectConfirmDialog = false
                }
            }
        }
    }

    @CircuitInject(CalendarSyncScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): CalendarSyncPresenter
    }
}
