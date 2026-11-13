package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Response wrapper for `GET /api/devices/{device_id}/forecast`.
 */
@Serializable
data class DeviceForecastResponse(
    val data: DeviceForecast,
)

/**
 * Forecast data containing upcoming check-in steps for a device.
 */
@Serializable
data class DeviceForecast(
    val steps: List<TimelineStep> = emptyList(),
)

/**
 * A projected or scheduled device wake-up and screen render step.
 */
@Serializable
data class TimelineStep(
    val at: String,
    @SerialName("playlist_item_id")
    val playlistItemId: Int? = null,
    @SerialName("plugin_setting_id")
    val pluginSettingId: Int? = null,
    val name: String? = null,
    @SerialName("refresh_seconds")
    val refreshSeconds: Int? = null,
    @SerialName("refresh_rate_seconds")
    val refreshRateSeconds: Int? = null,
    val reason: String? = null,
    @SerialName("render_at")
    val renderAt: String? = null,
    @SerialName("render_reason")
    val renderReason: String? = null,
)

/**
 * Response wrapper for `GET /api/devices/{device_id}/timeline`.
 */
@Serializable
data class DeviceTimelineResponse(
    val data: DeviceTimeline,
)

/**
 * Device activity timeline combining recorded events, projected steps, and check-in status.
 */
@Serializable
data class DeviceTimeline(
    val date: String,
    val today: String,
    val zone: String,
    val available: Boolean,
    @SerialName("query_failed")
    val queryFailed: Boolean = false,
    val truncated: Boolean = false,
    @SerialName("check_in")
    val checkIn: TimelineCheckIn? = null,
    val recorded: List<TimelineRecordedItem> = emptyList(),
    val expected: List<TimelineStep> = emptyList(),
    val strip: List<TimelineStep> = emptyList(),
)

/**
 * Current device check-in health summary from the timeline endpoint.
 */
@Serializable
data class TimelineCheckIn(
    val state: String,
    @SerialName("expected_at")
    val expectedAt: String? = null,
)

/**
 * A recorded timeline entry, including primary/supporting log objects and trace entries.
 */
@Serializable
data class TimelineRecordedItem(
    val at: String,
    val name: String? = null,
    val outcome: String? = null,
    val leading: JsonObject? = null,
    val supporting: List<JsonObject> = emptyList(),
    @SerialName("trace_entries")
    val traceEntries: List<JsonElement> = emptyList(),
)

/**
 * Response wrapper for `GET /api/devices/{device_id}/logs`.
 */
@Serializable
data class DeviceLogsResponse(
    val data: DeviceLogsData,
)

/**
 * Paginated device log entries and cursor metadata.
 */
@Serializable
data class DeviceLogsData(
    val logs: List<JsonObject> = emptyList(),
    val more: Boolean = false,
    @SerialName("oldest_cursor")
    val oldestCursor: DeviceLogCursor? = null,
)

/**
 * Cursor for loading older log entries via `before_ts` and `before_event_id`.
 */
@Serializable
data class DeviceLogCursor(
    val ts: String,
    @SerialName("event_id")
    val eventId: String,
)
