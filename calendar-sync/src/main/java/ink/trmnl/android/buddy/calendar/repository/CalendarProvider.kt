package ink.trmnl.android.buddy.calendar.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import ink.trmnl.android.buddy.calendar.models.SyncEvent
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Wrapper around Android Calendar Provider for reading calendar and event data.
 *
 * PERMISSIONS: Requires READ_CALENDAR permission.
 * Does NOT request WRITE_CALENDAR permission.
 *
 * See: https://developer.android.com/guide/topics/providers/calendar-provider
 */
class CalendarProvider(
    private val context: Context,
) {
    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        private const val TAG = "CalendarProvider"

        // Projection for calendar query
        private val CALENDARS_PROJECTION =
            arrayOf(
                CalendarContract.Calendars._ID, // 0
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, // 1
                CalendarContract.Calendars.ACCOUNT_NAME, // 2
                CalendarContract.Calendars.ACCOUNT_TYPE, // 3
                CalendarContract.Calendars.OWNER_ACCOUNT, // 4
                CalendarContract.Calendars.CALENDAR_COLOR, // 5
            )

        private const val CALENDARS_ID_INDEX = 0
        private const val CALENDARS_DISPLAY_NAME_INDEX = 1
        private const val CALENDARS_ACCOUNT_NAME_INDEX = 2
        private const val CALENDARS_ACCOUNT_TYPE_INDEX = 3
        private const val CALENDARS_OWNER_ACCOUNT_INDEX = 4
        private const val CALENDARS_COLOR_INDEX = 5

        // Projection for events query
        private val EVENTS_PROJECTION =
            arrayOf(
                CalendarContract.Events._ID, // 0
                CalendarContract.Events.TITLE, // 1
                CalendarContract.Events.DESCRIPTION, // 2
                CalendarContract.Events.DTSTART, // 3
                CalendarContract.Events.DTEND, // 4
                CalendarContract.Events.ALL_DAY, // 5
                CalendarContract.Events.EVENT_TIMEZONE, // 6
                CalendarContract.Events.CALENDAR_ID, // 7
                CalendarContract.Events.AVAILABILITY, // 8
            )

        private const val EVENTS_ID_INDEX = 0
        private const val EVENTS_TITLE_INDEX = 1
        private const val EVENTS_DESCRIPTION_INDEX = 2
        private const val EVENTS_DTSTART_INDEX = 3
        private const val EVENTS_DTEND_INDEX = 4
        private const val EVENTS_ALL_DAY_INDEX = 5
        private const val EVENTS_TIMEZONE_INDEX = 6
        private const val EVENTS_CALENDAR_ID_INDEX = 7
        private const val EVENTS_AVAILABILITY_INDEX = 8
    }

    /**
     * Get all calendars available on the device.
     *
     * Returns calendars from all account types (Google, Outlook, Exchange, CalDAV, local, etc.)
     *
     * @return List of available calendars, empty if none found or on permission error
     */
    fun getAllCalendars(): List<SyncCalendar> =
        try {
            val uri = CalendarContract.Calendars.CONTENT_URI
            val cursor: Cursor? = contentResolver.query(uri, CALENDARS_PROJECTION, null, null, null)

            cursor?.use {
                val calendars = mutableListOf<SyncCalendar>()
                while (it.moveToNext()) {
                    try {
                        val calendar =
                            SyncCalendar(
                                calendarId = it.getLong(CALENDARS_ID_INDEX),
                                calendarName = it.getStringOrNull(CALENDARS_DISPLAY_NAME_INDEX) ?: "Unknown",
                                accountName = it.getStringOrNull(CALENDARS_ACCOUNT_NAME_INDEX) ?: "Unknown",
                                accountType = it.getStringOrNull(CALENDARS_ACCOUNT_TYPE_INDEX) ?: "Unknown",
                                ownerAccount = it.getStringOrNull(CALENDARS_OWNER_ACCOUNT_INDEX) ?: "Unknown",
                                calendarColor = it.getIntOrNull(CALENDARS_COLOR_INDEX),
                                isSelected = false,
                            )
                        calendars.add(calendar)
                        Timber.d("[$TAG] Found calendar: ${calendar.calendarName} (${calendar.accountName})")
                    } catch (e: Exception) {
                        Timber.e(e, "[$TAG] Error parsing calendar row")
                    }
                }
                calendars
            } ?: emptyList()
        } catch (e: SecurityException) {
            Timber.e(e, "[$TAG] READ_CALENDAR permission denied")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Error querying calendars")
            emptyList()
        }

    /**
     * Get events for specific calendars within a time window.
     *
     * Matches iOS Companion app logic: past 7 days to future 30 days.
     *
     * @param calendarIds List of calendar IDs to query (empty = all calendars)
     * @param startTime Events after this timestamp (milliseconds since epoch). Default: 7 days ago
     * @param endTime Events before this timestamp (milliseconds since epoch). Default: 30 days from now
     * @return List of events in TRMNL format
     */
    fun getEventsForCalendars(
        calendarIds: List<Long>,
        startTime: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // 7 days ago
        endTime: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000), // 30 days from now
    ): List<SyncEvent> =
        try {
            val uri = CalendarContract.Events.CONTENT_URI

            // Build selection: calendar IDs AND time range
            val selection =
                if (calendarIds.isNotEmpty()) {
                    val idPlaceholders = calendarIds.joinToString(",") { "?" }
                    "((${CalendarContract.Events.CALENDAR_ID} IN ($idPlaceholders)) AND " +
                        "(${CalendarContract.Events.DTSTART} > ?) AND " +
                        "(${CalendarContract.Events.DTSTART} < ?))"
                } else {
                    "((${CalendarContract.Events.DTSTART} > ?) AND " +
                        "(${CalendarContract.Events.DTSTART} < ?))"
                }

            val selectionArgs =
                if (calendarIds.isNotEmpty()) {
                    calendarIds.map { it.toString() }.toTypedArray() + arrayOf(startTime.toString(), endTime.toString())
                } else {
                    arrayOf(startTime.toString(), endTime.toString())
                }

            val cursor: Cursor? =
                contentResolver.query(
                    uri,
                    EVENTS_PROJECTION,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC",
                )

            cursor?.use {
                val events = mutableListOf<SyncEvent>()
                while (it.moveToNext()) {
                    try {
                        val event = parseEventRow(it)
                        if (event != null) {
                            events.add(event)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "[$TAG] Error parsing event row")
                    }
                }
                Timber.d("[$TAG] Found ${events.size} events in time window")
                events
            } ?: emptyList()
        } catch (e: SecurityException) {
            Timber.e(e, "[$TAG] READ_CALENDAR permission denied")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Error querying events")
            emptyList()
        }

    /**
     * Parse a calendar event row from Cursor into SyncEvent format.
     *
     * Converts Android Calendar Provider format to TRMNL API format.
     */
    private fun parseEventRow(cursor: Cursor): SyncEvent? {
        val title = cursor.getStringOrNull(EVENTS_TITLE_INDEX) ?: return null
        val description = cursor.getStringOrNull(EVENTS_DESCRIPTION_INDEX)
        val dtStart = cursor.getLongOrNull(EVENTS_DTSTART_INDEX) ?: return null
        val dtEnd = cursor.getLongOrNull(EVENTS_DTEND_INDEX) ?: dtStart
        val allDay = cursor.getIntOrNull(EVENTS_ALL_DAY_INDEX)?.let { it == 1 } ?: false
        val timezone = cursor.getStringOrNull(EVENTS_TIMEZONE_INDEX) ?: TimeZone.getDefault().id
        val calendarId = cursor.getLongOrNull(EVENTS_CALENDAR_ID_INDEX) ?: return null

        // Format times
        val startFull = formatToISO8601(dtStart, timezone)
        val endFull = formatToISO8601(dtEnd, timezone)
        val startTime = formatTime(dtStart, timezone)
        val endTime = formatTime(dtEnd, timezone)

        // Get calendar name from ID
        val calendarName =
            getCalendarNameById(calendarId)
                ?: calendarId.toString()

        // Determine attendance status (simplified - Android doesn't have direct status field for organizer)
        val status = "accepted"

        return SyncEvent(
            startTime = startTime,
            endTime = endTime,
            startFull = startFull,
            endFull = endFull,
            dateTime = startFull,
            summary = title,
            description = description,
            allDay = allDay,
            status = status,
            calendarName = calendarName,
        )
    }

    /**
     * Get calendar name/email for a calendar ID.
     */
    private fun getCalendarNameById(calendarId: Long): String? =
        try {
            val uri = CalendarContract.Calendars.CONTENT_URI
            val selection = "${CalendarContract.Calendars._ID} = ?"
            val selectionArgs = arrayOf(calendarId.toString())
            val cursor: Cursor? =
                contentResolver.query(
                    uri,
                    arrayOf(CalendarContract.Calendars.OWNER_ACCOUNT),
                    selection,
                    selectionArgs,
                    null,
                )

            cursor?.use {
                if (it.moveToFirst()) {
                    it.getStringOrNull(0)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Error getting calendar name for ID $calendarId")
            null
        }

    /**
     * Format timestamp to ISO 8601 with timezone (e.g., "2025-10-02T16:00:00.000Z")
     */
    private fun formatToISO8601(
        timeMillis: Long,
        timezone: String,
    ): String =
        try {
            val sdf =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone(timezone)
                }
            sdf.format(timeMillis)
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Error formatting ISO8601 datetime")
            ""
        }

    /**
     * Format timestamp to time string (HH:mm format, e.g., "16:00")
     */
    private fun formatTime(
        timeMillis: Long,
        timezone: String,
    ): String =
        try {
            val sdf =
                SimpleDateFormat("HH:mm", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone(timezone)
                }
            sdf.format(timeMillis)
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Error formatting time")
            ""
        }
}
