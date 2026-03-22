package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single calendar event to be synced to the TRMNL server.
 *
 * @property title The title or summary of the event
 * @property startTime ISO 8601 formatted start date/time (e.g., "2025-01-15T09:00:00Z")
 * @property endTime ISO 8601 formatted end date/time (e.g., "2025-01-15T10:00:00Z")
 * @property location Optional location of the event
 * @property description Optional description or notes for the event
 * @property allDay Whether the event spans the entire day (default: false)
 */
@Serializable
data class CalendarEvent(
    @SerialName("title")
    val title: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("location")
    val location: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("all_day")
    val allDay: Boolean = false,
)
