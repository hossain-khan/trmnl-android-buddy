package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wrapper model for calendar events in the TRMNL Companion API format.
 *
 * Used as the value for [CalendarSyncRequest.mergeVariables].
 *
 * @property events List of calendar events to sync
 */
@Serializable
data class MergeVariables(
    @SerialName("events")
    val events: List<CalendarEvent>,
)

/**
 * Request body for syncing calendar events to the TRMNL server.
 *
 * Used with the POST /plugin_settings/{id}/data endpoint to send calendar data from
 * the Android device to TRMNL devices via the calendar plugin.
 *
 * Matches the TRMNL Companion app (iOS) API specification.
 *
 * @property mergeVariables Wrapper containing the list of calendar events to sync
 */
@Serializable
data class CalendarSyncRequest(
    @SerialName("merge_variables")
    val mergeVariables: MergeVariables,
)
