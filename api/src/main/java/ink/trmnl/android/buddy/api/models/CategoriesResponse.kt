package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response wrapper for the `/categories` API endpoint.
 *
 * Returns a list of valid plugin categories that can be used to improve
 * search exposure and filtering for Public or Recipe style plugins.
 *
 * Example response:
 * ```json
 * {
 *   "data": [
 *     "analytics",
 *     "art",
 *     "calendar",
 *     "comics",
 *     "crm",
 *     "custom",
 *     "discovery",
 *     "ecommerce",
 *     "education",
 *     "email",
 *     "entertainment",
 *     "environment",
 *     "finance",
 *     "games",
 *     "humor",
 *     "images",
 *     "kpi",
 *     "life",
 *     "marketing",
 *     "nature",
 *     "news",
 *     "personal",
 *     "productivity",
 *     "programming",
 *     "sales",
 *     "sports",
 *     "travel"
 *   ]
 * }
 * ```
 *
 * @property data List of category identifiers (strings)
 */
@Serializable
data class CategoriesResponse(
    @SerialName("data")
    val data: List<String>,
)
