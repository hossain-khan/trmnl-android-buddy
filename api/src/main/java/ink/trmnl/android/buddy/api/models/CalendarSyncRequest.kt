package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for syncing calendar events to the TRMNL server.
 *
 * Used with the POST /calendar/sync endpoint to send calendar data from
 * the Android device to TRMNL devices via the calendar plugin.
 *
 * @property events List of calendar events to sync
 */
@Serializable
data class CalendarSyncRequest(
    @SerialName("events")
    val events: List<CalendarEvent>,
)
