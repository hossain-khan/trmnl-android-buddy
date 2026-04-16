package ink.trmnl.android.buddy.ui.recipesanalytics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data Transfer Object for displaying recipes analytics in the UI.
 *
 * This simplified DTO is created from [RecipesAnalyticsResponse] and contains only
 * the data necessary for UI display. Being Parcelable, it can be safely passed
 * through Circuit screen navigation.
 */
@Parcelize
data class RecipesAnalyticsUi(
    /** Total number of plugins published by user */
    val totalPlugins: Int,
    /** Number of connections across all plugins */
    val totalConnections: Int,
    /** Total page views across all plugins */
    val totalPageviews: Int,
    /** Percentage of plugins in healthy state */
    val healthyPercent: Double,
    /** Percentage of plugins in degraded state */
    val degradedPercent: Double,
    /** Percentage of plugins in erroring state */
    val erroringPercent: Double,
    /** Growth data points for last 8 days */
    val growthData: List<GrowthDataPointUi>,
    /** List of published plugins with their stats */
    val plugins: List<PluginAnalyticsUi>,
) : Parcelable

/**
 * UI representation of a growth data point.
 *
 * @property date Date in format "YYYY-MM-DD"
 * @property value Number of new connections or activity on that date
 */
@Parcelize
data class GrowthDataPointUi(
    val date: String,
    val value: Int,
) : Parcelable

/**
 * UI representation of a plugin with analytics stats.
 *
 * @property name Plugin name/title
 * @property state Health state: "healthy", "degraded", or "erroring"
 * @property installs Number of installations
 * @property forks Number of forks/copies
 */
@Parcelize
data class PluginAnalyticsUi(
    val name: String,
    val state: String,
    val installs: Int,
    val forks: Int,
) : Parcelable
