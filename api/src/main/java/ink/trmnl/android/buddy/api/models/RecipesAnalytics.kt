package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for growth data points.
 * Deserializes from JSON arrays like ["2026-04-09", 0] into typed GrowthDataPoint objects.
 */
object GrowthDataPointSerializer : KSerializer<GrowthDataPoint> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("GrowthDataPoint")

    override fun serialize(
        encoder: Encoder,
        value: GrowthDataPoint,
    ) {
        // For now, we only need deserialization support
        error("GrowthDataPoint serialization not supported")
    }

    override fun deserialize(decoder: Decoder): GrowthDataPoint {
        require(decoder is JsonDecoder)
        val array = decoder.decodeJsonElement() as JsonArray
        return GrowthDataPoint(
            date = array[0].jsonPrimitive.content,
            value = array[1].jsonPrimitive.content.toInt(),
        )
    }
}

/**
 * Represents a single data point in the growth history.
 * Deserializes from JSON arrays like ["2026-04-09", 0] to typed pair.
 *
 * @property date Date of the data point (ISO 8601 format)
 * @property value Number of installs or connections on that date
 */
@Serializable(with = GrowthDataPointSerializer::class)
data class GrowthDataPoint(
    val date: String,
    val value: Int,
)

/**
 * Represents analytics data for recipes and plugins from the TRMNL catalog.
 *
 * @property plugins List of plugins with their health status, install count, and forks
 * @property stats Aggregate statistics for all plugins
 * @property health Health status breakdown with percentages
 * @property growth Historical growth data as date-value pairs
 */
@Serializable
data class RecipesAnalytics(
    val plugins: List<RecipeAnalyticsPlugin>,
    val stats: RecipeAnalyticsStats,
    val health: RecipeAnalyticsHealth,
    val growth: List<GrowthDataPoint>,
)

/**
 * Individual plugin analytics data.
 *
 * @property name Plugin name
 * @property state Health state of the plugin (e.g., "healthy", "degraded", "erroring")
 * @property installs Number of installations
 * @property forks Number of forks
 */
@Serializable
data class RecipeAnalyticsPlugin(
    val name: String,
    val state: String,
    val installs: Int,
    val forks: Int,
)

/**
 * Aggregate statistics for recipes and plugins.
 *
 * @property plugins Total number of plugins
 * @property connections Number of connections
 * @property pageviews Number of pageviews
 */
@Serializable
data class RecipeAnalyticsStats(
    val plugins: Int,
    val connections: Int,
    val pageviews: Int,
)

/**
 * Health status breakdown for recipes and plugins.
 *
 * @property healthy Percentage of healthy plugins
 * @property degraded Percentage of degraded plugins
 * @property erroring Percentage of erroring plugins
 */
@Serializable
data class RecipeAnalyticsHealth(
    val healthy: RecipeAnalyticsHealthStatus,
    val degraded: RecipeAnalyticsHealthStatus,
    val erroring: RecipeAnalyticsHealthStatus,
)

/**
 * Health status percentage.
 *
 * @property percent Percentage value (nullable for unauthenticated responses)
 */
@Serializable
data class RecipeAnalyticsHealthStatus(
    val percent: Double?,
)

/**
 * Response wrapper for the `/analytics.json` API endpoint.
 *
 * Returns analytics data for the TRMNL recipes and plugins catalog,
 * including plugin health status, installation statistics, and growth trends.
 *
 * Example response:
 * ```json
 * {
 *   "data": {
 *     "plugins": [
 *       {
 *         "name": "Calendar XL",
 *         "state": "healthy",
 *         "installs": 0,
 *         "forks": 43
 *       }
 *     ],
 *     "stats": {
 *       "plugins": 9,
 *       "connections": 423,
 *       "pageviews": 28
 *     },
 *     "health": {
 *       "healthy": {"percent": 121.33},
 *       "degraded": {"percent": 0.67},
 *       "erroring": {"percent": 0.33}
 *     },
 *     "growth": [
 *       ["2026-04-09", 0],
 *       ["2026-04-10", 0]
 *     ]
 *   }
 * }
 * ```
 *
 * @see ApiResponse
 */
typealias RecipesAnalyticsResponse = ApiResponse<RecipesAnalytics>
