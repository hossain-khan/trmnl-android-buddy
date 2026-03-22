package ink.trmnl.android.buddy.calendar.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a calendar available on the device for syncing.
 *
 * Maps to Android's CalendarContract.Calendars columns.
 *
 * @property calendarId Unique calendar ID from Android Calendar Provider
 * @property calendarName Display name of the calendar
 * @property accountName Email/account name (e.g., "user@gmail.com")
 * @property accountType Account type (e.g., "com.google", "com.microsoft.outlook")
 * @property ownerAccount Account that owns the calendar
 * @property calendarColor Color of the calendar (as integer)
 * @property isSelected Whether this calendar should be synced to TRMNL
 */
@Serializable
data class SyncCalendar(
    val calendarId: Long,
    val calendarName: String,
    val accountName: String,
    val accountType: String,
    val ownerAccount: String,
    val calendarColor: Int? = null,
    val isSelected: Boolean = false,
) {
    /**
     * Friendly identifier for this calendar combining account and calendar name.
     */
    fun getId(): String = "$accountName:$calendarId"
}

/**
 * Represents a synced calendar event to be sent to TRMNL API.
 *
 * Maps Android Calendar Provider event data to TRMNL calendar plugin format.
 *
 * Data format matches TRMNL iOS Companion app:
 * https://help.trmnl.com/en/articles/12294875-trmnl-companion-for-ios-calendar-sync
 */
@Serializable
data class SyncEvent(
    /**
     * Event time in HH:mm format (e.g., "16:00")
     */
    @SerialName("start")
    val startTime: String,
    /**
     * Event end time in HH:mm format (e.g., "18:00")
     */
    @SerialName("end")
    val endTime: String,
    /**
     * Full ISO 8601 datetime with timezone (e.g., "2025-10-02T16:00:00.000Z")
     */
    @SerialName("start_full")
    val startFull: String,
    /**
     * Full ISO 8601 datetime with timezone (e.g., "2025-10-02T18:00:00.000Z")
     */
    @SerialName("end_full")
    val endFull: String,
    /**
     * Start datetime same as start_full for convenience (e.g., "2025-10-02T16:00:00.000Z")
     */
    @SerialName("date_time")
    val dateTime: String,
    /**
     * Event title/summary (e.g., "Support chats")
     */
    @SerialName("summary")
    val summary: String,
    /**
     * Event description/notes
     */
    @SerialName("description")
    val description: String? = null,
    /**
     * Whether this is an all-day event
     */
    @SerialName("all_day")
    val allDay: Boolean = false,
    /**
     * Attendee status: "accepted", "declined", "tentative", "needs_action", etc.
     */
    @SerialName("status")
    val status: String = "accepted",
    /**
     * Calendar name/identifier for filtering (e.g., "ryan@usetrmnl")
     */
    @SerialName("calname")
    val calendarName: String,
)

/**
 * Request payload for syncing calendars to TRMNL API.
 */
@Serializable
data class CalendarSyncRequest(
    /**
     * List of events to sync
     */
    val events: List<SyncEvent>,
    /**
     * List of calendar IDs being synced (for TRMNL to filter by)
     */
    val calendarIds: List<String>? = null,
)

/**
 * Response from TRMNL API after calendar sync.
 */
@Serializable
data class CalendarSyncResponse(
    /**
     * Status message
     */
    val status: String,
    /**
     * Number of events synced
     */
    val eventsSynced: Int? = null,
    /**
     * Any error message if sync failed
     */
    val error: String? = null,
)
