package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single calendar event to be synced to the TRMNL server.
 *
 * Matches the TRMNL Companion app (iOS) API specification.
 *
 * @property summary The title or summary of the event
 * @property start Event start time in HH:mm 24-hour format (e.g., "14:30")
 * @property startFull Full ISO 8601 datetime with timezone (e.g., "2025-08-24T14:30:00.000-04:00")
 * @property dateTime ISO 8601 datetime with timezone, same as [startFull] (e.g., "2025-08-24T14:30:00.000-04:00")
 * @property end Event end time in HH:mm 24-hour format (e.g., "15:30")
 * @property endFull Full ISO 8601 end datetime with timezone (e.g., "2025-08-24T15:30:00.000-04:00")
 * @property allDay Whether the event spans the entire day (default: false)
 * @property description Optional description or notes for the event
 * @property status Event status: "confirmed" or "tentative" (default: "confirmed")
 * @property calendarIdentifier Unique calendar identifier (e.g., "user@example.com")
 */
@Serializable
data class CalendarEvent(
    @SerialName("summary")
    val summary: String,
    @SerialName("start")
    val start: String,
    @SerialName("start_full")
    val startFull: String,
    @SerialName("date_time")
    val dateTime: String,
    @SerialName("end")
    val end: String,
    @SerialName("end_full")
    val endFull: String,
    @SerialName("all_day")
    val allDay: Boolean = false,
    @SerialName("description")
    val description: String = "",
    @SerialName("status")
    val status: String = "confirmed",
    @SerialName("calendar_identifier")
    val calendarIdentifier: String,
)
