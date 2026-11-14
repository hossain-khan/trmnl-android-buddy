package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * Represents a failing plugin install entry returned by `GET /api/analytics/errors`.
 *
 * @property kind Plugin kind (`"recipe"` or `"third_party"`).
 * @property subjectId Numeric identifier of the recipe or third-party plugin.
 * @property pluginName Display name of the plugin, if available.
 * @property owner Fault attribution (`"author"`, `"upstream"`, `"installer_connection"`, or `"installer_setup"`).
 * @property message Error message associated with the failure.
 * @property count Number of occurrences observed in the reporting window.
 * @property lastSeen ISO-8601 timestamp when the error was last seen.
 * @property hidden Whether the error has been hidden by the author.
 */
@Serializable
data class AuthorPluginError(
    val kind: String,
    @SerialName("subject_id")
    val subjectId: Int,
    @SerialName("plugin_name")
    val pluginName: String? = null,
    val owner: String,
    val message: String,
    val count: Int,
    @SerialName("last_seen")
    val lastSeen: String,
    val hidden: Boolean = false,
)

/**
 * Response wrapper for `GET /api/analytics/errors`.
 */
typealias AuthorPluginErrorsResponse = ApiResponse<List<AuthorPluginError>>

/**
 * Custom serializer for uninstall reason breakdown tuples `[reason, count]`.
 */
object UninstallReasonBreakdownSerializer : KSerializer<UninstallReasonBreakdown> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("UninstallReasonBreakdown")

    override fun serialize(
        encoder: Encoder,
        value: UninstallReasonBreakdown,
    ) {
        error("UninstallReasonBreakdown serialization not supported")
    }

    override fun deserialize(decoder: Decoder): UninstallReasonBreakdown {
        require(decoder is JsonDecoder)
        val array = decoder.decodeJsonElement() as JsonArray
        return UninstallReasonBreakdown(
            reason = array[0].jsonPrimitive.content,
            count = array[1].jsonPrimitive.int,
        )
    }
}

/**
 * Aggregated uninstall reason and occurrence count from `[reason, count]` JSON arrays.
 *
 * @property reason Human-readable uninstall reason label.
 * @property count Number of uninstalls citing this reason.
 */
@Serializable(with = UninstallReasonBreakdownSerializer::class)
data class UninstallReasonBreakdown(
    val reason: String,
    val count: Int,
)

/**
 * Individual free-text comment left by a user upon uninstalling an author's plugin.
 *
 * @property detail Comment text entered by the user.
 * @property pluginName Name of the uninstalled plugin.
 * @property reasonLabel Selected uninstall reason label, if any.
 * @property kindLabel Human-readable plugin kind label, if any.
 * @property createdAt ISO-8601 timestamp when the feedback was submitted.
 */
@Serializable
data class UninstallComment(
    val detail: String,
    @SerialName("plugin_name")
    val pluginName: String,
    @SerialName("reason_label")
    val reasonLabel: String? = null,
    @SerialName("kind_label")
    val kindLabel: String? = null,
    @SerialName("created_at")
    val createdAt: String,
)

/**
 * Aggregated uninstall feedback summary for the authenticated author's plugins.
 *
 * @property windowDays Number of days covered by the feedback window (typically 30).
 * @property total Total number of uninstall feedback entries in the window.
 * @property reasonBreakdown Breakdown of uninstall counts by reason.
 * @property recentComments Recent free-text uninstall comments.
 * @property typicalDaysInstalled Median days installed prior to uninstall, if available.
 */
@Serializable
data class UninstallFeedback(
    @SerialName("window_days")
    val windowDays: Int,
    val total: Int,
    @SerialName("reason_breakdown")
    val reasonBreakdown: List<UninstallReasonBreakdown> = emptyList(),
    @SerialName("recent_comments")
    val recentComments: List<UninstallComment> = emptyList(),
    @SerialName("typical_days_installed")
    val typicalDaysInstalled: Int? = null,
)

/**
 * Response wrapper for `GET /api/analytics/uninstall_feedback`.
 */
typealias UninstallFeedbackResponse = ApiResponse<UninstallFeedback>
