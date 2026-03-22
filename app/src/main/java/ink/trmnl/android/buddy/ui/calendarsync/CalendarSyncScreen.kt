package ink.trmnl.android.buddy.ui.calendarsync

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import kotlinx.parcelize.Parcelize

/**
 * Screen for managing calendar sync settings.
 *
 * **Features:**
 * - Display list of available device calendars with selection toggles
 * - Handle READ_CALENDAR permission via Accompanist Permissions API
 * - Trigger immediate calendar sync ("Sync Now")
 * - Refresh list of available calendars
 * - Disconnect calendar sync with confirmation dialog
 * - Display last sync time, sync status, and error messages
 *
 * **Permission Flow:**
 * 1. Permission not granted → Show "Request Permission" button
 * 2. Permission denied once → Show rationale dialog explaining why READ_CALENDAR is needed
 * 3. Permission granted → Show calendar list and sync controls
 *
 * @see CalendarSyncPresenter Business logic and state management
 * @see CalendarSyncContent UI rendering with permission handling
 */
@Parcelize
data object CalendarSyncScreen : Screen {
    /**
     * UI state for the calendar sync screen.
     *
     * @property isLoading True while calendars are being loaded from the device
     * @property calendars List of available calendars with selection state
     * @property isSyncEnabled Whether calendar sync is currently enabled
     * @property lastSyncTime Timestamp of last successful sync (ms since epoch, 0 if never)
     * @property lastSyncError Error message from last failed sync, or null if no error
     * @property syncStatus Current sync operation status
     * @property showDisconnectConfirmDialog Whether the disconnect confirmation dialog is visible
     * @property errorMessage General error message for display (e.g., calendar load failures)
     * @property eventSink Handler for user interaction events
     */
    data class State(
        val isLoading: Boolean = false,
        val calendars: List<SyncCalendar> = emptyList(),
        val isSyncEnabled: Boolean = false,
        val lastSyncTime: Long = 0L,
        val lastSyncError: String? = null,
        val syncStatus: SyncStatus = SyncStatus.Idle,
        val showDisconnectConfirmDialog: Boolean = false,
        val errorMessage: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Sync operation status.
     */
    enum class SyncStatus {
        /** No sync operation in progress */
        Idle,

        /** Sync operation currently running */
        Syncing,

        /** Last sync operation failed */
        Error,
    }

    /**
     * Events that can be triggered from the calendar sync screen UI.
     */
    sealed class Event : CircuitUiEvent {
        /** User tapped the back button. Navigates to previous screen. */
        data object BackClicked : Event()

        /** User toggled calendar selection. */
        data class CalendarToggled(
            val calendarId: Long,
            val selected: Boolean,
        ) : Event()

        /** User tapped "Sync Now" to trigger an immediate sync. */
        data object SyncNowClicked : Event()

        /** User tapped "Refresh Calendars" to reload available calendars. */
        data object RefreshCalendarsClicked : Event()

        /** User tapped "Disconnect Sync" button, showing confirmation dialog. */
        data object DisconnectClicked : Event()

        /** User confirmed disconnecting calendar sync. */
        data object DisconnectConfirmed : Event()

        /** User dismissed the disconnect confirmation dialog. */
        data object DisconnectDismissed : Event()
    }
}
